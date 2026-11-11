package com.aetherius.compat;

import com.aetherius.Aetherius;

/**
 * Optional TerraBlender integration point for injecting <b>Starlit Meadows</b> and
 * <b>Blighted Barrens</b> into the overworld's multi-noise biome source.
 *
 * <p>Fabric has no vanilla-supported way to add a biome to the overworld's noise parameters at
 * runtime. Three supported strategies, in order of preference:</p>
 *
 * <ol>
 *   <li><b>TerraBlender</b> (recommended): add the dependency in {@code build.gradle}, then
 *       register a {@code Region} whose {@code addBiomes} adds {@code aetherius:starlit_meadows}
 *       and {@code aetherius:blighted_barrens} with weights.</li>
 *   <li><b>Datapack</b>: ship an override of
 *       {@code data/minecraft/worldgen/multi_noise_biome_source_parameter_list/overworld.json}
 *       containing the extra entries. Simple, but conflicts with other worldgen datapacks.</li>
 *   <li><b>Runtime transformation</b>: this mod's own fallback - the blight system converts
 *       terrain texture and mobs in place, so an infected region <i>behaves</i> like Blighted
 *       Barrens without needing a dedicated biome ID.</li>
 * </ol>
 *
 * <p>This class exists so the rest of the codebase never has to reference TerraBlender types
 * directly: call {@link #isAvailable()} before attempting a region registration.</p>
 */
public final class TerraBlenderCompat {

    private static final boolean AVAILABLE = detect();

    private TerraBlenderCompat() {
    }

    private static boolean detect() {
        try {
            Class.forName("terrablender.api.TerraBlenderApi");
            Aetherius.LOGGER.info("[Aetherius] TerraBlender detected - Starlit Meadows can be "
                    + "injected into the overworld.");
            return true;
        } catch (ClassNotFoundException ignored) {
            Aetherius.LOGGER.info("[Aetherius] TerraBlender not present - Starlit Meadows biome "
                    + "stays data-only; blight provides the regional transformation instead.");
            return false;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }
}
