package com.aetherius.compat;

import com.aetherius.Aetherius;

/**
 * Cheap runtime check for TerraBlender, used for log lines and guide text.
 *
 * <p>The actual integration lives in {@link AetheriusTerraBlender}, which TerraBlender loads
 * through the {@code terrablender} entrypoint in {@code fabric.mod.json}; that class registers
 * {@link AetheriusOverworldRegion}, which adds {@code aetherius:starlit_meadows} and
 * {@code aetherius:blighted_barrens} to the overworld's multi-noise parameter list.</p>
 *
 * <p>Keeping the entrypoint in a separate class is what makes the dependency optional: nothing
 * outside {@code compat/} references a {@code terrablender} type, so the mod still loads when the
 * library is absent. {@link #isAvailable()} answers "is the library on this installation?" for
 * anything that wants to word a message differently - never as a reason to skip registration.</p>
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
