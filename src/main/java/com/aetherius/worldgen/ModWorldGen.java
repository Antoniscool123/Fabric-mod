package com.aetherius.worldgen;

import com.aetherius.dimension.ShatteredSkyChunkGenerator;
import com.aetherius.registry.ModBlocks;
import com.aetherius.util.Aeth;
import com.aetherius.worldgen.feature.ManaLilyPatchFeature;
import com.aetherius.worldgen.feature.StarlightObservatoryFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.BlockMatchRuleTest;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;

import java.util.List;

/**
 * Code-driven world generation.
 *
 * <p>Everything here is registered in Java instead of data JSON because the features are
 * parameterised by Aetherius blocks and the observatory writes to
 * {@link com.aetherius.world.AetheriusWorldState} at generation time. The equivalent JSON
 * lives in {@code data/aetherius/} only for the biomes and the dimension itself.</p>
 *
 * <h2>Generation</h2>
 * <ul>
 *   <li><b>Star-Vein Ore</b>: rare, deepslate band, y = -40..20, 6 veins/chunk.</li>
 *   <li><b>Aether-Blood Ore</b>: rarer, y = -60..-10, 3 veins/chunk.</li>
 *   <li><b>Starlight Observatory</b>: 1-in-140 chunks, surface structure step.</li>
 *   <li><b>Mana Lilies</b>: patch feature inside the Starlit Meadows biome.</li>
 * </ul>
 */
public final class ModWorldGen {

    private static final RuleTest STONE_ORE_REPLACEABLES = new BlockMatchRuleTest(net.minecraft.block.Blocks.STONE);
    private static final RuleTest DEEPSLATE_ORE_REPLACEABLES =
            new BlockMatchRuleTest(net.minecraft.block.Blocks.DEEPSLATE);

    // ---- Custom features --------------------------------------------------
    public static final Feature<DefaultFeatureConfig> STARLIGHT_OBSERVATORY =
            new StarlightObservatoryFeature(DefaultFeatureConfig.CODEC);
    public static final Feature<DefaultFeatureConfig> MANA_LILY_PATCH =
            new ManaLilyPatchFeature(DefaultFeatureConfig.CODEC);

    // ---- Configured features ---------------------------------------------
    private static ConfiguredFeature<?, ?> starVeinConfigured;
    private static ConfiguredFeature<?, ?> aetherBloodConfigured;
    private static ConfiguredFeature<?, ?> observatoryConfigured;
    private static ConfiguredFeature<?, ?> manaLilyConfigured;

    // ---- Placed feature keys (also referenced from the biome JSON) --------
    public static final RegistryKey<PlacedFeature> STAR_VEIN_PLACED =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Aeth.id("star_vein_ore"));
    public static final RegistryKey<PlacedFeature> AETHER_BLOOD_PLACED =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Aeth.id("aether_blood_ore"));
    public static final RegistryKey<PlacedFeature> OBSERVATORY_PLACED =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Aeth.id("starlight_observatory"));
    public static final RegistryKey<PlacedFeature> MANA_LILY_PLACED =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Aeth.id("mana_lily_patch"));

    private ModWorldGen() {
    }

    public static void register() {
        Registry.register(Registries.FEATURE, Aeth.id("starlight_observatory"), STARLIGHT_OBSERVATORY);
        Registry.register(Registries.FEATURE, Aeth.id("mana_lily_patch"), MANA_LILY_PATCH);

        // The custom chunk generator must be registered before any dimension tries to use it.
        Registry.register(Registries.CHUNK_GENERATOR, Aeth.id("shattered_sky"),
                ShatteredSkyChunkGenerator.CODEC);

        starVeinConfigured = registerConfigured("star_vein_ore",
                new OreFeatureConfig(List.of(OreFeatureConfig.createTarget(STONE_ORE_REPLACEABLES,
                                ModBlocks.STAR_VEIN_ORE.getDefaultState()),
                        OreFeatureConfig.createTarget(DEEPSLATE_ORE_REPLACEABLES,
                                ModBlocks.STAR_VEIN_ORE.getDefaultState())),
                        6));
        aetherBloodConfigured = registerConfigured("aether_blood_ore",
                new OreFeatureConfig(List.of(OreFeatureConfig.createTarget(DEEPSLATE_ORE_REPLACEABLES,
                        ModBlocks.AETHER_BLOOD_ORE.getDefaultState())), 3));
        observatoryConfigured = registerConfigured("starlight_observatory",
                STARLIGHT_OBSERVATORY, DefaultFeatureConfig.INSTANCE);
        manaLilyConfigured = registerConfigured("mana_lily_patch",
                MANA_LILY_PATCH, DefaultFeatureConfig.INSTANCE);

        registerPlaced("star_vein_ore", starVeinConfigured,
                CountPlacementModifier.of(6),
                SquarePlacementModifier.of(),
                HeightRangePlacementModifier.uniform(YOffset.fixed(-48), YOffset.fixed(24)),
                BiomePlacementModifier.of());
        registerPlaced("aether_blood_ore", aetherBloodConfigured,
                CountPlacementModifier.of(3),
                SquarePlacementModifier.of(),
                HeightRangePlacementModifier.uniform(YOffset.fixed(-60), YOffset.fixed(-12)),
                BiomePlacementModifier.of());
        registerPlaced("starlight_observatory", observatoryConfigured,
                RarityFilterPlacementModifier.of(140),
                SquarePlacementModifier.of(),
                HeightRangePlacementModifier.uniform(YOffset.fixed(60), YOffset.fixed(140)),
                BiomePlacementModifier.of());
        registerPlaced("mana_lily_patch", manaLilyConfigured,
                CountPlacementModifier.of(8),
                SquarePlacementModifier.of(),
                HeightRangePlacementModifier.uniform(YOffset.fixed(60), YOffset.fixed(120)),
                BiomePlacementModifier.of());

        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, STAR_VEIN_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES, AETHER_BLOOD_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.SURFACE_STRUCTURES, OBSERVATORY_PLACED);
    }

    private static ConfiguredFeature<?, ?> registerConfigured(String path, OreFeatureConfig config, int ignored) {
        return Registry.register(Registries.CONFIGURED_FEATURE, Aeth.id(path),
                new ConfiguredFeature<>(Feature.ORE, config));
    }

    private static ConfiguredFeature<?, ?> registerConfigured(String path, Feature<DefaultFeatureConfig> feature,
                                                             DefaultFeatureConfig config) {
        return Registry.register(Registries.CONFIGURED_FEATURE, Aeth.id(path),
                new ConfiguredFeature<>(feature, config));
    }

    private static void registerPlaced(String path, ConfiguredFeature<?, ?> configured,
                                       net.minecraft.world.gen.placementmodifier.PlacementModifier... modifiers) {
        Registry.register(Registries.PLACED_FEATURE, Aeth.id(path),
                new PlacedFeature(RegistryEntry.of(configured), List.of(modifiers)));
    }

    /** Convenience for the /aetherius locate command. */
    public static List<RegistryKey<PlacedFeature>> allPlaced() {
        return List.of(STAR_VEIN_PLACED, AETHER_BLOOD_PLACED, OBSERVATORY_PLACED, MANA_LILY_PLACED);
    }

    /** Utility used by the observatory feature to pick an ore count without a data file. */
    public static UniformIntProvider veinSize(int min, int max) {
        return UniformIntProvider.create(min, max);
    }
}
