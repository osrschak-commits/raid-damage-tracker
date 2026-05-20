package com.raiddamagetracker;

import java.util.Set;

/**
 * NPC IDs for Chambers of Xeric bosses and significant enemies.
 * Includes both normal and Challenge Mode variants.
 */
public final class CoxNpcs
{
    private CoxNpcs() {}

    // Great Olm (head + hands)
    public static final int OLM_HEAD            = 7551;
    public static final int OLM_LEFT_HAND       = 7552;
    public static final int OLM_RIGHT_HAND      = 7553;
    // CM variants
    public static final int OLM_HEAD_CM         = 7554;
    public static final int OLM_LEFT_HAND_CM    = 7555;
    public static final int OLM_RIGHT_HAND_CM   = 7556;

    // Tekton
    public static final int TEKTON              = 7540;
    public static final int TEKTON_ENRAGED      = 7541;
    public static final int TEKTON_CM           = 7542;
    public static final int TEKTON_ENRAGED_CM   = 7543;

    // Vespula
    public static final int VESPULA             = 7531;
    public static final int VESPULA_CM          = 7532;
    public static final int VESPINE_SOLDIER     = 7533;

    // Vasa Nistirio
    public static final int VASA_NISTIRIO       = 7567;
    public static final int VASA_NISTIRIO_CM    = 7568;

    // Vanguards
    public static final int VANGUARD_MELEE      = 7526;
    public static final int VANGUARD_RANGED     = 7527;
    public static final int VANGUARD_MAGE       = 7528;
    public static final int VANGUARD_MELEE_CM   = 7529; // shared CM IDs vary; adjust if needed
    public static final int VANGUARD_RANGED_CM  = 7530;

    // Skeletal Mystics
    public static final int SKELETAL_MYSTIC     = 7605;
    public static final int SKELETAL_MYSTIC_CM  = 7606;

    // Lizardman Shamans
    public static final int LIZARDMAN_SHAMAN    = 7573;

    // Guardians
    public static final int GUARDIAN            = 7570;
    public static final int GUARDIAN_CM         = 7571;

    // Muttadile
    public static final int MUTTADILE_SMALL     = 7562;
    public static final int MUTTADILE_LARGE     = 7563;
    public static final int MUTTADILE_SMALL_CM  = 7564;
    public static final int MUTTADILE_LARGE_CM  = 7565;

    // Deathly Mage / Ranger (ice demon room)
    public static final int ICE_DEMON           = 7584;
    public static final int ICE_DEMON_CM        = 7585;

    private static final Set<Integer> TRACKED = Set.of(
            OLM_HEAD, OLM_LEFT_HAND, OLM_RIGHT_HAND,
            OLM_HEAD_CM, OLM_LEFT_HAND_CM, OLM_RIGHT_HAND_CM,
            TEKTON, TEKTON_ENRAGED, TEKTON_CM, TEKTON_ENRAGED_CM,
            VESPULA, VESPULA_CM, VESPINE_SOLDIER,
            VASA_NISTIRIO, VASA_NISTIRIO_CM,
            VANGUARD_MELEE, VANGUARD_RANGED, VANGUARD_MAGE,
            VANGUARD_MELEE_CM, VANGUARD_RANGED_CM,
            SKELETAL_MYSTIC, SKELETAL_MYSTIC_CM,
            LIZARDMAN_SHAMAN,
            GUARDIAN, GUARDIAN_CM,
            MUTTADILE_SMALL, MUTTADILE_LARGE,
            MUTTADILE_SMALL_CM, MUTTADILE_LARGE_CM,
            ICE_DEMON, ICE_DEMON_CM
    );

    public static boolean isTracked(int npcId) { return TRACKED.contains(npcId); }
}
