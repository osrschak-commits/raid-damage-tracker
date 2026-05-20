package com.raiddamagetracker;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Side panel — layout varies by DPS mode:
 *
 * BOSS_SPLIT
 * ┌─────────────────────────────┐
 * │ Raid Damage Tracker         │
 * │ Raid: Theatre of Blood      │
 * │                    [Reset]  │
 * ├─────────────────────────────┤
 * │ ▶ Maiden  (live)            │  ← NPC damage card (your dmg + %)
 * ├─────────────────────────────┤
 * │ DPS Log                     │
 * │  Maiden   1,823 dmg  3.12/s │  ← completed boss records
 * │  Bloat    2,011 dmg  4.55/s │
 * └─────────────────────────────┘
 *
 * RAID_OVERALL
 * ┌─────────────────────────────┐
 * │ Raid Damage Tracker         │
 * │ Raid: Chambers of Xeric     │
 * │ Overall DPS: 3.45           │
 * │ Total dmg:  9,124           │
 * │                    [Reset]  │
 * ├─────────────────────────────┤
 * │ ▶ Tekton  (live)            │
 * └─────────────────────────────┘
 */
public class RaidDamageTrackerPanel extends PluginPanel
{
    // Colours
    private static final Color HEADER_BG        = new Color(30, 30, 30);
    private static final Color SECTION_BG       = new Color(40, 40, 40);
    private static final Color ROW_BG_EVEN      = new Color(45, 45, 45);
    private static final Color ROW_BG_ODD       = new Color(50, 50, 50);
    private static final Color DEAD_COLOR       = new Color(180, 80,  80);
    private static final Color LIVE_COLOR       = new Color(80,  180, 80);
    private static final Color DPS_COLOR        = new Color(100, 200, 255);
    private static final Color GOLD             = new Color(255, 200, 80);
    private static final Color DIVIDER_COLOR    = new Color(60, 60, 60);
    private static final Color LOG_HEADER_BG    = new Color(35, 35, 35);

    private final RaidDamageTrackerConfig config;
    private RaidDamageTrackerPlugin plugin;   // set via setter after injection

    // ── Header widgets ────────────────────────────────────────────────────────
    private final JLabel raidLabel    = new JLabel("Not in a raid");
    private final JLabel overallDps   = new JLabel("Overall DPS: —");
    private final JLabel overallTotal = new JLabel("Total dmg: 0");
    private final JButton resetBtn    = new JButton("Reset");

    // ── Body ──────────────────────────────────────────────────────────────────
    private final JPanel contentPanel = new JPanel();

    // ── NPC cards (live + dead damage display) ────────────────────────────────
    /** npcKey → card component */
    private final Map<String, NpcCard> npcCards = new LinkedHashMap<>();

    // ── BOSS_SPLIT log section ────────────────────────────────────────────────
    private JPanel dpsLogSection;      // the whole section; rebuilt on each update
    private DpsMode currentMode = DpsMode.BOSS_SPLIT;

    @Inject
    public RaidDamageTrackerPanel(RaidDamageTrackerConfig config)
    {
        this.config = config;

        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        add(buildHeader(), BorderLayout.NORTH);

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(contentPanel, BorderLayout.CENTER);
    }

    /** Called by the plugin after construction so we can trigger hardReset from the button. */
    public void setPlugin(RaidDamageTrackerPlugin plugin)
    {
        this.plugin = plugin;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Raid Damage Tracker");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        styleSmall(raidLabel,    Color.LIGHT_GRAY);
        styleSmall(overallDps,   DPS_COLOR);
        styleSmall(overallTotal, Color.LIGHT_GRAY);

        resetBtn.setFont(FontManager.getRunescapeSmallFont());
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setBackground(new Color(70, 30, 30));
        resetBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 50, 50)),
                new EmptyBorder(2, 8, 2, 8)));
        resetBtn.setFocusPainted(false);
        resetBtn.setAlignmentX(RIGHT_ALIGNMENT);
        resetBtn.addActionListener(e ->
        {
            if (plugin != null) plugin.hardReset();
        });

        // Reset row (right-aligned)
        JPanel resetRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        resetRow.setBackground(HEADER_BG);
        resetRow.setAlignmentX(LEFT_ALIGNMENT);
        resetRow.add(resetBtn);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(raidLabel);
        header.add(overallDps);    // only visible in RAID_OVERALL mode
        header.add(overallTotal);  // only visible in RAID_OVERALL mode
        header.add(Box.createVerticalStrut(4));
        header.add(resetRow);

        return header;
    }

    private void styleSmall(JLabel label, Color fg)
    {
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(fg);
        label.setAlignmentX(LEFT_ALIGNMENT);
    }

    // ── Public API (always called on EDT) ─────────────────────────────────────

    public void setRaid(RaidType type, DpsMode mode)
    {
        this.currentMode = mode;
        applyModeVisibility(mode);

        if (type == RaidType.NONE)
        {
            raidLabel.setText("Not in a raid");
            overallDps.setText("Overall DPS: —");
            overallTotal.setText("Total dmg: 0");
        }
        else
        {
            raidLabel.setText("Raid: " + type.getDisplayName()
                    + "  [" + mode.getDisplayName() + "]");
        }
        repaint();
    }

    public void reset()
    {
        npcCards.clear();
        contentPanel.removeAll();
        dpsLogSection = null;
        overallDps.setText("Overall DPS: —");
        overallTotal.setText("Total dmg: 0");
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── RAID_OVERALL ──────────────────────────────────────────────────────────

    public void updateOverallDps(int totalDamage, double dps)
    {
        overallDps.setText(String.format("Overall DPS: %.2f", dps));
        overallTotal.setText("Total dmg: " + formatDmg(totalDamage));
    }

    // ── BOSS_SPLIT ────────────────────────────────────────────────────────────

    /**
     * Rebuild the DPS log section at the bottom of the panel.
     * Called each time a boss dies with the full list of completed records.
     */
    public void setBossRecords(List<BossDpsRecord> records)
    {
        // Remove old section if present
        if (dpsLogSection != null)
        {
            contentPanel.remove(dpsLogSection);
        }

        dpsLogSection = buildDpsLogSection(records);
        contentPanel.add(dpsLogSection);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel buildDpsLogSection(List<BossDpsRecord> records)
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(SECTION_BG);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER_COLOR),
                new EmptyBorder(4, 6, 4, 6)));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        section.setAlignmentX(LEFT_ALIGNMENT);

        JLabel heading = new JLabel("DPS Log");
        heading.setFont(FontManager.getRunescapeBoldFont());
        heading.setForeground(DPS_COLOR);
        section.add(heading);
        section.add(Box.createVerticalStrut(4));

        // Column header
        section.add(buildLogRow("Boss", "Damage", "DPS", true));

        for (BossDpsRecord r : records)
        {
            section.add(buildLogRow(
                    r.bossName,
                    formatDmg(r.damage),
                    String.format("%.2f/s", r.getDps()),
                    false
            ));
        }

        return section;
    }

    private JPanel buildLogRow(String boss, String dmg, String dps, boolean header)
    {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(header ? LOG_HEADER_BG : ROW_BG_EVEN);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(LEFT_ALIGNMENT);

        Font font  = header ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont();
        Color fg   = header ? Color.WHITE : Color.LIGHT_GRAY;
        Color dpsFg = header ? Color.WHITE : DPS_COLOR;

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(1, 3, 1, 3);

        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        JLabel bossL = new JLabel(boss); bossL.setFont(font); bossL.setForeground(fg);
        row.add(bossL, gc);

        gc.gridx = 1; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        JLabel dmgL = new JLabel(dmg); dmgL.setFont(font); dmgL.setForeground(fg);
        row.add(dmgL, gc);

        gc.gridx = 2;
        JLabel dpsL = new JLabel(dps); dpsL.setFont(font); dpsL.setForeground(dpsFg);
        row.add(dpsL, gc);

        return row;
    }

    // ── Live NPC cards ────────────────────────────────────────────────────────

    /** Update the live card for a boss while it's still alive. */
    public void updateLiveNpc(String npcKey, int myDamage)
    {
        NpcCard card = npcCards.computeIfAbsent(npcKey, k ->
        {
            NpcCard c = new NpcCard(npcKey);
            // Insert before the DPS log section if present
            if (dpsLogSection != null)
            {
                int idx = getComponentIndex(contentPanel, dpsLogSection);
                contentPanel.add(c, idx >= 0 ? idx : contentPanel.getComponentCount());
            }
            else
            {
                contentPanel.add(c);
            }
            contentPanel.add(Box.createVerticalStrut(4));
            contentPanel.revalidate();
            return c;
        });
        card.update(myDamage, false);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /** Mark a card as dead and freeze its damage total. */
    public void onNpcDied(String npcKey, int myDamage)
    {
        NpcCard card = npcCards.computeIfAbsent(npcKey, k ->
        {
            NpcCard c = new NpcCard(npcKey);
            contentPanel.add(c);
            contentPanel.add(Box.createVerticalStrut(4));
            contentPanel.revalidate();
            return c;
        });
        card.update(myDamage, true);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyModeVisibility(DpsMode mode)
    {
        boolean overall = (mode == DpsMode.RAID_OVERALL);
        overallDps.setVisible(overall);
        overallTotal.setVisible(overall);
    }

    private static int getComponentIndex(Container container, Component target)
    {
        for (int i = 0; i < container.getComponentCount(); i++)
        {
            if (container.getComponent(i) == target) return i;
        }
        return -1;
    }

    private static String formatDmg(int dmg)
    {
        return String.format("%,d", dmg);
    }

    // ── Inner class: NpcCard ─────────────────────────────────────────────────

    private class NpcCard extends JPanel
    {
        private final JLabel statusLabel;
        private final JLabel damageLabel;
        private final JLabel pctLabel;

        NpcCard(String npcKey)
        {
            setLayout(new BorderLayout(0, 2));
            setBackground(SECTION_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DIVIDER_COLOR),
                    new EmptyBorder(4, 6, 4, 6)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            setAlignmentX(LEFT_ALIGNMENT);

            String displayName = npcKey.contains(" #")
                    ? npcKey.substring(0, npcKey.lastIndexOf(" #"))
                    : npcKey;

            // ─ Title row ─
            JPanel titleRow = new JPanel(new BorderLayout());
            titleRow.setBackground(SECTION_BG);

            JLabel nameLabel = new JLabel(displayName);
            nameLabel.setFont(FontManager.getRunescapeBoldFont());
            nameLabel.setForeground(Color.WHITE);

            statusLabel = new JLabel("⚔ Alive");
            statusLabel.setFont(FontManager.getRunescapeSmallFont());
            statusLabel.setForeground(LIVE_COLOR);

            titleRow.add(nameLabel,   BorderLayout.WEST);
            titleRow.add(statusLabel, BorderLayout.EAST);
            add(titleRow, BorderLayout.NORTH);

            // ─ Damage row ─
            JPanel dmgRow = new JPanel(new GridBagLayout());
            dmgRow.setBackground(ROW_BG_EVEN);
            dmgRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = new Insets(1, 3, 1, 3);

            JLabel youLabel = new JLabel("Your damage");
            youLabel.setFont(FontManager.getRunescapeSmallFont());
            youLabel.setForeground(GOLD);
            gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
            dmgRow.add(youLabel, gc);

            damageLabel = new JLabel("0");
            damageLabel.setFont(FontManager.getRunescapeSmallFont());
            damageLabel.setForeground(GOLD);
            gc.gridx = 1; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
            dmgRow.add(damageLabel, gc);

            pctLabel = new JLabel("");
            pctLabel.setFont(FontManager.getRunescapeSmallFont());
            pctLabel.setForeground(Color.GRAY);
            gc.gridx = 2;
            dmgRow.add(pctLabel, gc);

            add(dmgRow, BorderLayout.CENTER);
        }

        void update(int myDamage, boolean dead)
        {
            statusLabel.setText(dead ? "✗ Dead" : "⚔ Alive");
            statusLabel.setForeground(dead ? DEAD_COLOR : LIVE_COLOR);
            damageLabel.setText(formatDmg(myDamage));
            // Percentage doesn't apply when it's only your damage,
            // but keep the label in case we want to show % of boss HP in future.
            pctLabel.setText("");
        }
    }
}
