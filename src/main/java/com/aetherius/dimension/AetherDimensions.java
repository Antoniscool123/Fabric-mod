package com.aetherius.dimension;

import com.aetherius.util.Aeth;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

/**
 * Registry keys for the Shattered Sky, plus the constants the gravity mixin reads.
 *
 * <p>The dimension itself is data-driven ({@code data/aetherius/dimension/shattered_sky.json}
 * + {@code dimension_type/shattered_sky.json}) but the <b>low gravity</b> and temporal
 * storm behaviour are code-driven because vanilla dimension types have no gravity field.</p>
 */
public final class AetherDimensions {

    public static final RegistryKey<World> SHATTERED_SKY =
            RegistryKey.of(RegistryKeys.WORLD, Aeth.id("shattered_sky"));

    public static final RegistryKey<DimensionType> SHATTERED_SKY_TYPE =
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Aeth.id("shattered_sky"));

    /** Gravity multiplier inside the Shattered Sky. */
    public static final double LOW_GRAVITY = 0.35;

    /** How often (ticks) a temporal storm may start rolling. */
    public static final int TEMPORAL_STORM_INTERVAL = 20 * 60 * 4;

    private AetherDimensions() {
    }
}
