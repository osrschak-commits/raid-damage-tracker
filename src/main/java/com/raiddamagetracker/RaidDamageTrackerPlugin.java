package com.raiddamagetracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import net.runelite.api.events.GameTick;

@Slf4j
@PluginDescriptor(
        name = "Raid Damage Tracker",
        description = "Tracks your damage on each boss in CoX, ToB, and ToA raids with configurable DPS modes",
        tags = {"raid", "damage", "tracker", "cox", "tob", "toa", "dps", "chambers", "theatre", "tombs"}
)
public class RaidDamageTrackerPlugin extends Plugin
{
    // -------------------------------------------------------------------------
    // Raid region IDs
    // -------------------------------------------------------------------------
    private static final int COX_REGION       = 12889;
    private static final int COX_LOBBY_REGION = 12890;
    private static final int TOB_REGION       = 12611;
    private static final int TOB_LOBBY_REGION = 14642;
    private static final int TOA_REGION       = 14160;
    private static final int TOA_LOBBY_REGION = 13454;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private RaidDamageTrackerConfig config;

    private RaidDamageTrackerPanel panel;
    private NavigationButton navButton;

    // -------------------------------------------------------------------------
    // Raid state
    // -------------------------------------------------------------------------
    private RaidType currentRaid = RaidType.NONE;

    /**
     * Per-NPC damage tracking (your hits only).
     * Key: "NpcName #index"  Value: total damage you dealt
     */
    private final Map<String, Integer> damageByNpc = new LinkedHashMap<>();

    /** Maps NPC index → canonical key so we can look up on hitsplat/despawn. */
    private final Map<Integer, String> npcKeys = new HashMap<>();

    // -------------------------------------------------------------------------
    // BOSS_SPLIT mode state
    // -------------------------------------------------------------------------

    /**
     * For each tracked NPC, the time (ms) we first dealt damage to it.
     * Used as the start of the boss fight window for DPS calculation.
     */
    private final Map<Integer, Long> bossFirstHitTime = new HashMap<>();

    /**
     * Completed boss DPS records, in kill order.
     * Persists until the user clicks "Reset" in the panel.
     */
    private final List<BossDpsRecord> bossRecords = new ArrayList<>();

    // -------------------------------------------------------------------------
    // RAID_OVERALL mode state
    // -------------------------------------------------------------------------

    /**
     * Total damage dealt to all bosses across the raid (your hits only).
     * Resets when a new raid starts or user hits Reset.
     */
    private int overallTotalDamage = 0;

    /**
     * Accumulated active-combat milliseconds.
     * The timer runs only while at least one tracked boss is alive in the room
     * (i.e. in npcKeys), so inter-room transitions don't inflate the denominator.
     */
    private long overallAccumulatedMs = 0;

    /**
     * Timestamp (ms) when the current combat window started, or -1 if paused.
     * Set to System.currentTimeMillis() when the first boss spawns in a room;
     * set to -1 when the last tracked boss in the room despawns.
     */
    private long overallCombatWindowStart = -1;

    // -------------------------------------------------------------------------

    @Override
    protected void startUp()
    {
        panel = injector.getInstance(RaidDamageTrackerPanel.class);
        panel.setPlugin(this);   // needed so the Reset button can call hardReset()

        final BufferedImage icon = ImageUtil.loadImageResource(
                getClass(), "/com/raiddamagetracker/icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Raid Damage Tracker")
                .icon(icon)
                .priority(6)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        log.info("Raid Damage Tracker started");
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
        hardReset();
        log.info("Raid Damage Tracker stopped");
    }

    @Provides
    RaidDamageTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RaidDamageTrackerConfig.class);
    }

    // -------------------------------------------------------------------------
    // Region / raid detection
    // -------------------------------------------------------------------------

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOADING)
        {
            updateRaidType();
        }
    }

    private void updateRaidType()
    {
        if (client.getLocalPlayer() == null) return;

        int region = client.getLocalPlayer().getWorldLocation().getRegionID();
        RaidType detected = RaidType.NONE;

        // Use varbits for reliable raid detection across all worlds including Leagues
        // Varbit 5424 = CoX raid state (1+ = in raid)
        // Varbit 6447 = ToB raid state (1+ = in raid)
        // Varbit 14345 = ToA raid state (1+ = in raid)
        int coxVarbit  = client.getVarbitValue(5424);
        int tobVarbit  = client.getVarbitValue(6447);
        int toaVarbit  = client.getVarbitValue(14345);

        if      (coxVarbit > 0) detected = RaidType.COX;
        else if (tobVarbit > 0) detected = RaidType.TOB;
        else if (toaVarbit > 0) detected = RaidType.TOA;

        //log.info("updateRaidType:",

        if (detected == currentRaid) return;

        if (detected != RaidType.NONE)
        {
            startRaid(detected);
        }
        else
        {
            endRaid();
        }
        currentRaid = detected;
    }

    private void startRaid(RaidType type)
    {
        damageByNpc.clear();
        npcKeys.clear();
        bossFirstHitTime.clear();
        bossRecords.clear();
        overallTotalDamage = 0;
        overallAccumulatedMs = 0;
        overallCombatWindowStart = -1;
        currentRaid = type;
        SwingUtilities.invokeLater(() -> { panel.reset(); panel.setRaid(type, config.dpsMode()); });
        log.info("Entered raid: {}", type);

        // Scan for already-alive bosses on the client thread
        clientThread.invokeLater(() ->
        {
            if (client.getNpcs() == null) return;
            for (NPC npc : client.getNpcs())
            {
                if (npc == null) continue;
                if (!isTrackedNpc(npc)) continue;
                String key = npcKey(npc);
                npcKeys.put(npc.getIndex(), key);
                damageByNpc.put(key, 0);
                SwingUtilities.invokeLater(() -> panel.updateLiveNpc(key, 0));
            }
            if (config.dpsMode() == DpsMode.RAID_OVERALL && !npcKeys.isEmpty())
            {
                overallCombatWindowStart = System.currentTimeMillis();
            }
        });
    }

    private void endRaid()
    {
        log.info("Left raid: {}", currentRaid);
        pauseOverallTimer();          // stop the clock if it was running
        currentRaid = RaidType.NONE;
        SwingUtilities.invokeLater(() -> panel.setRaid(RaidType.NONE, config.dpsMode()));
    }

    /**
     * Full reset — clears everything including the persisted boss records.
     * Called on new raid entry, plugin shutdown, and the panel Reset button.
     */
    public void hardReset()
    {
        damageByNpc.clear();
        npcKeys.clear();
        bossFirstHitTime.clear();
        bossRecords.clear();
        overallTotalDamage = 0;
        overallAccumulatedMs = 0;
        overallCombatWindowStart = -1;
        SwingUtilities.invokeLater(() -> panel.reset());

        // Repopulate npcKeys from currently alive NPCs — must run on client thread
        if (currentRaid != RaidType.NONE)
        {
            clientThread.invokeLater(() ->
            {
                if (client.getNpcs() == null) return;
                for (NPC npc : client.getNpcs())
                {
                    if (npc == null) continue;
                    if (!isTrackedNpc(npc)) continue;
                    String key = npcKey(npc);
                    npcKeys.put(npc.getIndex(), key);
                    damageByNpc.put(key, 0);
                }
                if (config.dpsMode() == DpsMode.RAID_OVERALL && !npcKeys.isEmpty())
                {
                    overallCombatWindowStart = System.currentTimeMillis();
                }
            });
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        updateRaidType();
    }
// -------------------------------------------------------------------------
    // NPC tracking
    // -------------------------------------------------------------------------

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        if (currentRaid == RaidType.NONE) return;

        NPC npc = event.getNpc();
        if (!isTrackedNpc(npc)) return;

        String key = npcKey(npc);
        npcKeys.put(npc.getIndex(), key);
        damageByNpc.putIfAbsent(key, 0);

        // Show the NPC card immediately in the panel (damage starts at 0)
        SwingUtilities.invokeLater(() -> panel.updateLiveNpc(key, 0));

        // RAID_OVERALL: start the combat window when the first boss spawns in the room
        if (config.dpsMode() == DpsMode.RAID_OVERALL && overallCombatWindowStart == -1)
        {
            overallCombatWindowStart = System.currentTimeMillis();
            log.debug("Overall timer resumed — boss spawned: {}", key);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        if (currentRaid == RaidType.NONE) return;

        NPC npc = event.getNpc();
        String key = npcKeys.remove(npc.getIndex());
        if (key == null) return;

        int myDamage = damageByNpc.getOrDefault(key, 0);

        // ---- BOSS_SPLIT: record DPS for this boss ----
        if (config.dpsMode() == DpsMode.BOSS_SPLIT && myDamage > 0)
        {
            Long firstHit = bossFirstHitTime.remove(npc.getIndex());
            long durationMs = firstHit != null
                    ? System.currentTimeMillis() - firstHit
                    : 0;

            String displayName = displayName(key);
            BossDpsRecord record = new BossDpsRecord(displayName, myDamage, durationMs);
            bossRecords.add(record);

            final List<BossDpsRecord> snapshot = new ArrayList<>(bossRecords);
            SwingUtilities.invokeLater(() -> panel.setBossRecords(snapshot));
            log.info("Boss killed: {} {} dmg in {}ms ({} dps)",
                    displayName, myDamage, durationMs, String.format("%.2f", record.getDps()));
        }

        // ---- RAID_OVERALL: pause timer if no bosses left in room ----
        if (config.dpsMode() == DpsMode.RAID_OVERALL && npcKeys.isEmpty())
        {
            pauseOverallTimer();
            log.debug("Overall timer paused — no bosses remain in room");
        }

        // Update the NPC's damage card to show it as dead
        final String finalKey = key;
        final int finalDmg = myDamage;
        SwingUtilities.invokeLater(() -> panel.onNpcDied(finalKey, finalDmg));
    }

    // -------------------------------------------------------------------------
    // Hitsplat — local player only
    // -------------------------------------------------------------------------

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (currentRaid == RaidType.NONE) return;
        if (!(event.getActor() instanceof NPC)) return;

        NPC npc = (NPC) event.getActor();
        String key = npcKeys.get(npc.getIndex());
        if (key == null) return;

        Hitsplat hitsplat = event.getHitsplat();
        if (!hitsplat.isMine()) return;   // only track our own hits

        int damage = hitsplat.getAmount();
        if (damage <= 0) return;

        // Record first-hit timestamp for BOSS_SPLIT DPS window
        bossFirstHitTime.putIfAbsent(npc.getIndex(), System.currentTimeMillis());

        // Accumulate damage
        damageByNpc.merge(key, damage, Integer::sum);
        int npcTotal = damageByNpc.get(key);

        // RAID_OVERALL accumulation
        overallTotalDamage += damage;

        // Push panel updates
        final String finalKey = key;
        final int finalNpcTotal = npcTotal;

        if (config.dpsMode() == DpsMode.BOSS_SPLIT)
        {
            SwingUtilities.invokeLater(() -> panel.updateLiveNpc(finalKey, finalNpcTotal));
        }
        else
        {
            double dps = computeOverallDps();
            final double finalDps = dps;
            SwingUtilities.invokeLater(() ->
            {
                panel.updateLiveNpc(finalKey, finalNpcTotal);
                panel.updateOverallDps(overallTotalDamage, finalDps);
            });
        }
    }

    // -------------------------------------------------------------------------
    // RAID_OVERALL helpers
    // -------------------------------------------------------------------------

    private void pauseOverallTimer()
    {
        if (overallCombatWindowStart == -1) return;
        overallAccumulatedMs += System.currentTimeMillis() - overallCombatWindowStart;
        overallCombatWindowStart = -1;
    }

    private double computeOverallDps()
    {
        long total = overallAccumulatedMs;
        if (overallCombatWindowStart != -1)
        {
            total += System.currentTimeMillis() - overallCombatWindowStart;
        }
        double seconds = total / 1000.0;
        return seconds > 0 ? overallTotalDamage / seconds : 0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String npcKey(NPC npc)
    {
        return npc.getName() + " #" + npc.getIndex();
    }

    private String displayName(String npcKey)
    {
        int idx = npcKey.lastIndexOf(" #");
        return idx >= 0 ? npcKey.substring(0, idx) : npcKey;
    }

    private boolean isTrackedNpc(NPC npc)
    {
        if (npc.getName() == null) return false;
        int id = npc.getId();
        if (currentRaid == RaidType.COX) return CoxNpcs.isTracked(id);
        if (currentRaid == RaidType.TOB) return TobNpcs.isTracked(id);
        if (currentRaid == RaidType.TOA) return ToaNpcs.isTracked(id);
        return false;
    }
}
