package com.raiddamagetracker;

public enum DpsMode
{
    BOSS_SPLIT("Per-Boss DPS"),
    RAID_OVERALL("Overall Raid DPS");

    private final String displayName;

    DpsMode(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
