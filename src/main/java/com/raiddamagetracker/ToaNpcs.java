package com.raiddamagetracker;

import java.util.Set;

/**
 * NPC IDs for Tombs of Amascut bosses.
 * Covers all invocation levels (the same NPC IDs scale with invocation level).
 */
public final class ToaNpcs
{
    private ToaNpcs() {}

    // Apmeken's Path — Zebak (Crocodile)
    public static final int ZEBAK               = 11730;

    // Apmeken's Path — Ba-Ba (Baboon)
    public static final int BABA                = 11778;
    public static final int BABA_BOULDER        = 11780; // optional

    // Scabaras' Path — Kephri (Scarab)
    public static final int KEPHRI              = 11719;
    public static final int KEPHRI_SHIELDED     = 11721;

    // Scabaras' Path — Akkha (Sphinx)
    public static final int AKKHA               = 11789;
    public static final int AKKHA_SHADOW        = 11791; // shadow clone

    // Het's Path — Het Puzzle (Tumeken's Shadow reflection — no boss)
    // Elidinis' Path — Warden (Wardens)
    public static final int ELIDINIS_WARDEN_P1  = 11753; // Obelisk
    public static final int ELIDINIS_WARDEN_P2  = 11756;
    public static final int ELIDINIS_WARDEN_P3  = 11759;
    public static final int TUMEKENS_WARDEN_P1  = 11762; // Obelisk
    public static final int TUMEKENS_WARDEN_P2  = 11765;
    public static final int TUMEKENS_WARDEN_P3  = 11770;

    // Core (Warden P2 core)
    public static final int WARDEN_CORE         = 11773;

    private static final Set<Integer> TRACKED = Set.of(
            ZEBAK,
            BABA, BABA_BOULDER,
            KEPHRI, KEPHRI_SHIELDED,
            AKKHA, AKKHA_SHADOW,
            ELIDINIS_WARDEN_P1, ELIDINIS_WARDEN_P2, ELIDINIS_WARDEN_P3,
            TUMEKENS_WARDEN_P1, TUMEKENS_WARDEN_P2, TUMEKENS_WARDEN_P3,
            WARDEN_CORE
    );

    public static boolean isTracked(int npcId) { return TRACKED.contains(npcId); }
}
