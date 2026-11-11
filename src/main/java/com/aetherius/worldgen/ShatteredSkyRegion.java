package com.aetherius.worldgen;

import com.aetherius.dimension.ShatteredSkyChunkGenerator;

/**
 * The three regions of the Shattered Sky, chosen from the same noise the terrain uses.
 *
 * <p>Biomes in vanilla are picked by a climate sampler; a custom dimension with a custom chunk
 * generator does not have one, so the region is derived here instead: a low-frequency "blight"
 * field carves out the {@link #BARRENS}, and everything else is split by altitude into meadows
 * and the broken highlands. The chunk generator uses exactly this method to pick surface blocks,
 * which is why a biome always matches the ground it sits on.</p>
 */
public enum ShatteredSkyRegion {

    /** Lush, walkable, oak-and-lily ground. */
    MEADOWS("aetherius:starlit_meadows"),

    /** Pale ash plateaus and pillars, high above the sea. */
    HIGHLANDS("aetherius:shattered_sky"),

    /** Corrupted ground where the blight has taken hold. */
    BARRENS("aetherius:blighted_barrens");

    /** Island surface altitude at which the highlands start. */
    public static final int HIGHLAND_HEIGHT = 96;

    private final String biomeId;

    ShatteredSkyRegion(String biomeId) {
        this.biomeId = biomeId;
    }

    public String biomeId() {
        return biomeId;
    }

    /** Region for a column. Pure, deterministic, safe to call from worker threads. */
    public static ShatteredSkyRegion at(int x, int z) {
        long seed = ShatteredSkyChunkGenerator.worldSeed();
        double blight = SkyNoise.fbm(x, z, seed ^ 0x51EEDL, 0.0022, 4);
        double ragged = SkyNoise.fbm(x, z, seed ^ 0x77AAL, 0.02, 2);
        if (blight + (ragged - 0.5) * 0.14 > 0.54) {
            return BARRENS;
        }
        // Tall islands read as broken highlands; the void around them has no biome of its own,
        // so a void column simply inherits the meadow colouring for fog and ambience.
        int surface = ShatteredSkyChunkGenerator.surfaceHeight(x, z, seed);
        int reference = surface == ShatteredSkyChunkGenerator.NO_SURFACE ? 0 : surface;
        return reference >= HIGHLAND_HEIGHT ? HIGHLANDS : MEADOWS;
    }
}
