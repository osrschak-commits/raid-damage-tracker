package com.raiddamagetracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("raiddamagetracker")
public interface RaidDamageTrackerConfig extends Config
{
    @ConfigItem(
            keyName = "dpsMode",
            name = "DPS Mode",
            description = "<html><b>Per-Boss DPS:</b> logs your DPS for each boss individually when it dies; " +
                          "entries stay until you click Reset.<br>" +
                          "<b>Overall Raid DPS:</b> one running DPS counter for the whole raid; " +
                          "the timer pauses between boss rooms so lobby/transition time is excluded.</html>",
            position = 0
    )
    default DpsMode dpsMode()
    {
        return DpsMode.BOSS_SPLIT;
    }

    @ConfigItem(
            keyName = "showPercentages",
            name = "Show damage percentages",
            description = "Show your share of total damage as a percentage alongside the raw number",
            position = 1
    )
    default boolean showPercentages()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showDeadNpcs",
            name = "Keep dead NPC entries",
            description = "Keep killed NPC damage summaries visible in the panel throughout the raid",
            position = 2
    )
    default boolean showDeadNpcs()
    {
        return true;
    }
}
