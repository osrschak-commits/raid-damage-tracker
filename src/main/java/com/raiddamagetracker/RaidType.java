package com.raiddamagetracker;

public enum RaidType
{
    NONE("—"),
    COX("Chambers of Xeric"),
    TOB("Theatre of Blood"),
    TOA("Tombs of Amascut");

    private final String displayName;

    RaidType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
