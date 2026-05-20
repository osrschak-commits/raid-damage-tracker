package com.raiddamagetracker;

/**
 * Immutable snapshot of the player's DPS performance against a single boss.
 * Stored and displayed in BOSS_SPLIT mode after the boss dies.
 */
public class BossDpsRecord
{
    /** Display name of the boss (NPC name without the #index suffix). */
    public final String bossName;

    /** Total damage the local player dealt to this boss. */
    public final int damage;

    /**
     * Duration in milliseconds from when the boss spawned (first hit received)
     * to when it despawned (died).
     */
    public final long durationMs;

    public BossDpsRecord(String bossName, int damage, long durationMs)
    {
        this.bossName   = bossName;
        this.damage     = damage;
        this.durationMs = durationMs;
    }

    /** DPS as damage-per-second, or 0 if duration is zero. */
    public double getDps()
    {
        double seconds = durationMs / 1000.0;
        return seconds > 0 ? damage / seconds : 0;
    }
}
