package com.aetherius.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;

import java.util.List;
import java.util.stream.Stream;

/**
 * Picks one of the three Aetherius biomes per column.
 *
 * <h2>Why a custom source instead of {@code minecraft:multi_noise}</h2>
 * Multi-noise needs the world's climate sampler, which is built from a
 * {@code ChunkGeneratorSettings} that only vanilla noise generators have. This dimension has its
 * own terrain generator, so the sampler would be meaningless. Instead the region comes from the
 * same value-noise field the terrain uses ({@link ShatteredSkyRegion#at(int, int)}), which
 * guarantees that the biome always matches the ground the player is standing on: meadows are low
 * and green, highlands are the pale plateaus above {@link ShatteredSkyRegion#HIGHLAND_HEIGHT},
 * and the barrens follow the blight field.
 *
 * <h2>JSON</h2>
 * <pre>
 * "biome_source": {
 *   "type": "aetherius:shattered_sky",
 *   "biomes": ["aetherius:starlit_meadows", "aetherius:shattered_sky", "aetherius:blighted_barrens"]
 * }
 * </pre>
 * The order of {@code biomes} is the contract: meadows, highlands, barrens. The ids are resolved
 * by the dynamic registry, so a typo fails world loading with a clear "unknown biome" error rather
 * than silently generating the wrong biome.
 */
public class ShatteredSkyBiomeSource extends BiomeSource {

    private static final Codec<RegistryEntryList<Biome>> BIOMES =
            RegistryCodecs.entryList(RegistryKeys.BIOME);

    public static final Codec<ShatteredSkyBiomeSource> CODEC =
            BIOMES.fieldOf("biomes").xmap(ShatteredSkyBiomeSource::new, source -> source.biomes).codec();

    private final RegistryEntryList<Biome> biomes;
    private final RegistryEntry<Biome> meadows;
    private final RegistryEntry<Biome> highlands;
    private final RegistryEntry<Biome> barrens;

    public ShatteredSkyBiomeSource(RegistryEntryList<Biome> biomes) {
        this.biomes = biomes;
        List<RegistryEntry<Biome>> list = biomes.stream().toList();
        this.meadows = at(list, 0);
        this.highlands = at(list, 1);
        this.barrens = at(list, 2);
    }

    /** Tolerant lookup: a short list falls back to its first entry instead of throwing. */
    private static RegistryEntry<Biome> at(List<RegistryEntry<Biome>> list, int index) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException(
                    "aetherius:shattered_sky biome source needs at least one biome, got none");
        }
        return list.get(Math.min(index, list.size() - 1));
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return Stream.of(meadows, highlands, barrens);
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        return switch (ShatteredSkyRegion.at(x, z)) {
            case MEADOWS -> meadows;
            case HIGHLANDS -> highlands;
            case BARRENS -> barrens;
        };
    }

    /** Biomes this source can produce, in the order the JSON lists them (used by the debug dump). */
    public List<String> describe() {
        return List.of(meadows.getKey().map(key -> key.getValue().toString()).orElse("?"),
                highlands.getKey().map(key -> key.getValue().toString()).orElse("?"),
                barrens.getKey().map(key -> key.getValue().toString()).orElse("?"));
    }

}
