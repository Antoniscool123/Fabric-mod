package com.aetherius.compat;

import com.aetherius.util.Aeth;
import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

import static terrablender.api.ParameterUtils.Continentalness;
import static terrablender.api.ParameterUtils.Depth;
import static terrablender.api.ParameterUtils.Erosion;
import static terrablender.api.ParameterUtils.Humidity;
import static terrablender.api.ParameterUtils.ParameterPointListBuilder;
import static terrablender.api.ParameterUtils.Temperature;
import static terrablender.api.ParameterUtils.Weirdness;

/**
 * Injects an Aetherius biome into the <b>overworld's</b> multi-noise parameter list, using
 * TerraBlender.
 *
 * <h2>Why this is the only way</h2>
 * Fabric has no API for adding a new biome to the overworld: {@code BiomeModifications} can only
 * edit biomes that already generate, and the overworld's climate table is a vanilla preset that a
 * mod cannot append to. TerraBlender exists to do exactly this - it merges the parameter points of
 * registered regions into the overworld's source at world creation.
 *
 * <h2>Where the biomes land</h2>
 * Each region claims a climate window, so the biome appears where that window occurs but never
 * replaces a vanilla biome outright (TerraBlender's overlay picks between them by weight):
 *
 * <ul>
 *   <li><b>Starlit Meadows</b> - cool and wet inland, the same climate a meadow occupies.</li>
 *   <li><b>Blighted Barrens</b> - hot, arid, heavily eroded inland.</li>
 *   <li><b>The Shattered Sky</b> stays dimension-only: it is a sky biome and would look wrong
 *       on overworld ground.</li>
 * </ul>
 *
 * <h2>Soft dependency</h2>
 * This class is only loaded through the {@code terrablender} entrypoint, so the mod runs with or
 * without TerraBlender installed. See {@link TerraBlenderCompat}.
 */
public class AetheriusOverworldRegion extends Region {

    /** Which climate window this region claims. */
    public enum Window {
        MEADOWS,
        BARRENS
    }

    private final Window window;
    private final RegistryKey<Biome> biome;

    public AetheriusOverworldRegion(Identifier name, Window window, RegistryKey<Biome> biome, int weight) {
        super(name, RegionType.OVERWORLD, weight);
        this.window = window;
        this.biome = biome;
    }

    /** Registry key of one of this mod's biomes. */
    public static RegistryKey<Biome> key(String path) {
        return RegistryKey.of(RegistryKeys.BIOME, Aeth.id(path));
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
                          Consumer<Pair<MultiNoiseUtil.NoiseHypercube, RegistryKey<Biome>>> mapper) {
        ParameterPointListBuilder points = new ParameterPointListBuilder();
        switch (window) {
            case MEADOWS -> points
                    .temperature(Temperature.COOL, Temperature.NEUTRAL)
                    .humidity(Humidity.WET, Humidity.HUMID)
                    .continentalness(Continentalness.INLAND)
                    .erosion(Erosion.EROSION_3, Erosion.EROSION_4)
                    .depth(Depth.SURFACE)
                    .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING,
                            Weirdness.MID_SLICE_NORMAL_DESCENDING);
            case BARRENS -> points
                    .temperature(Temperature.HOT)
                    .humidity(Humidity.ARID, Humidity.DRY)
                    .continentalness(Continentalness.INLAND, Continentalness.FAR_INLAND)
                    .erosion(Erosion.EROSION_5, Erosion.EROSION_6)
                    .depth(Depth.SURFACE)
                    .weirdness(Weirdness.MID_SLICE_VARIANT_ASCENDING,
                            Weirdness.MID_SLICE_VARIANT_DESCENDING);
        }

        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();
        // ParameterPointListBuilder#build expands the cross product of the axes above, so one
        // call can produce several points - each of them maps to the same biome.
        points.build().forEach(point -> builder.add(point, biome));
        builder.build().forEach(mapper::accept);
    }
}
