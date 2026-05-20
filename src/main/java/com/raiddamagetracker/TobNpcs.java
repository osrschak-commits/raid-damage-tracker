package com.raiddamagetracker;

import java.util.Set;

/**
 * NPC IDs for Theatre of Blood bosses.
 * Includes normal, Hard Mode (HM), and story-mode variants where applicable.
 */
public final class TobNpcs
{
    private TobNpcs() {}

    // The Maiden of Sugadinti
    public static final int MAIDEN              = 8360;
    public static final int MAIDEN_HM           = 10822;

    // Pestilent Bloat
    public static final int BLOAT               = 8359;
    public static final int BLOAT_HM            = 10812;

    // Nylocas Vasilias
    public static final int NYLOCAS_VASILIAS            = 8355;
    public static final int NYLOCAS_VASILIAS_HM         = 10776;
    // Nylocas pillars / waves (optional — remove if too noisy)
    public static final int NYLOCAS_ISCHYROS_SM  = 8342;
    public static final int NYLOCAS_TOXOBOLOS_SM = 8343;
    public static final int NYLOCAS_HAGIOS_SM    = 8344;

    // Sotetseg
    public static final int SOTETSEG            = 8388;
    public static final int SOTETSEG_HM         = 10865;
    public static final int SOTETSEG_MAZE       = 8387; // untargetable maze form

    // Xarpus
    public static final int XARPUS              = 8338;
    public static final int XARPUS_HM           = 10767;

    // Verzik Vitur
    public static final int VERZIK_P1           = 8370;
    public static final int VERZIK_P2           = 8372;
    public static final int VERZIK_P3           = 8374;
    public static final int VERZIK_P1_HM        = 10830;
    public static final int VERZIK_P2_HM        = 10832;
    public static final int VERZIK_P3_HM        = 10835;

    private static final Set<Integer> TRACKED = Set.of(
            MAIDEN, MAIDEN_HM,
            BLOAT, BLOAT_HM,
            NYLOCAS_VASILIAS, NYLOCAS_VASILIAS_HM,
            NYLOCAS_ISCHYROS_SM, NYLOCAS_TOXOBOLOS_SM, NYLOCAS_HAGIOS_SM,
            SOTETSEG, SOTETSEG_HM,
            XARPUS, XARPUS_HM,
            VERZIK_P1, VERZIK_P2, VERZIK_P3,
            VERZIK_P1_HM, VERZIK_P2_HM, VERZIK_P3_HM
    );

    public static boolean isTracked(int npcId) { return TRACKED.contains(npcId); }
}
