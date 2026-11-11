package com.aetherius.api.chronicle;

/**
 * Canonical counter keys. Never inline a raw string in a quest definition - typos in
 * counter names fail silently, which is the worst kind of quest bug.
 */
public final class ChronicleCounters {
    private ChronicleCounters() {
    }

    public static final String STAR_VEIN_BROKEN = "star_vein_broken";
    public static final String STARFALL_WITNESSED = "starfall_witnessed";
    public static final String AETHER_BLOOD_MINED = "aether_blood_mined";
    public static final String VOID_SENTINEL_KILLED = "void_sentinel_killed";
    public static final String VOID_SPECTER_KILLED = "void_specter_killed";
    public static final String NOX_CASTS = "nox_casts";
    public static final String BLIGHT_CHUNKS_WITNESSED = "blight_chunks_witnessed";
    public static final String ALTAR_PURIFICATIONS = "altar_purifications";
    public static final String ABOMINATIONS_CLEANSED = "abominations_cleansed";
    public static final String WARDEN_SOL_KILLED = "warden_sol_killed";
    public static final String WARDEN_LUNA_KILLED = "warden_luna_killed";
    public static final String WARDEN_TERRA_KILLED = "warden_terra_killed";
    public static final String WARDEN_CAELUM_KILLED = "warden_caelum_killed";
    public static final String GRAVITY_ANCHORS_DESTROYED = "gravity_anchors_destroyed";
    public static final String OBSERVATORY_REPAIRED = "observatory_repaired";
    public static final String OBSERVATORY_VISITED = "observatory_visited";

    // Story flags (written by blocks, items and the boss; read by quests and spells).
    public static final String FLAG_ACT1_COMPLETE = "act1_complete";
    public static final String FLAG_ACT2_ATTUNED = "act2_attuned";
    public static final String FLAG_BLIGHT_WITNESSED = "blight_witnessed";
    public static final String FLAG_ALTAR_BUILT = "altar_built";
    public static final String FLAG_BLIGHT_PURIFIED = "blight_purified";
    public static final String FLAG_SKY_RIFT_OPEN = "sky_rift_open";
    public static final String FLAG_SHATTERED_SKY_REACHED = "shattered_sky_reached";
    public static final String FLAG_NOX_THAL_SLAIN = "nox_thal_slain";
    public static final String FLAG_OBSERVATORY_REPAIRED = "observatory_repaired";
    public static final String FLAG_ANCHOR_GROUNDED = "nox_grounded";
    public static final String FLAG_FORGE_BUILT = "cosmic_forge_built";
    public static final String FLAG_WARDENS_SLAIN = "wardens_slain";
}
