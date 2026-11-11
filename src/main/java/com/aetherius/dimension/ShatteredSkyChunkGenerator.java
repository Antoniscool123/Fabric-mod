package com.aetherius.dimension;

import com.aetherius.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.HeightLimitView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Procedural generator for <b>The Shattered Sky</b>: an archipelago of floating islands in
 * an otherwise empty sky.
 *
 * <h2>Algorithm</h2>
 * The world is divided into 56x56-block cells. Each cell deterministically hashes to at most
 * one island (centre, radius, altitude). A column's surface height is the tallest island
 * surface among the 3x3 cells around it, so islands can overlap into ridges without needing
 * a noise field. Cell (0,0) is pinned to a large spawn island so portal arrivals always land
 * somewhere solid.
 *
 * <h2>Version note</h2>
 * The chunk-generator contract changes between Minecraft versions. If a mapping bump breaks
 * compilation, the methods to re-check first are {@code populateNoise},
 * {@code generateFeatures} and {@code buildSurface} - their signatures are the volatile
 * part, not the island maths.
 */
public class ShatteredSkyChunkGenerator extends ChunkGenerator {

    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;
    public static final int SEA_LEVEL = 90;
    public static final int CELL_SIZE = 56;
    public static final int NO_SURFACE = Integer.MIN_VALUE;

    /** World seed, pushed in by the mod initialiser so island placement is deterministic. */
    private static volatile long worldSeed;

    public static void setWorldSeed(long seed) {
        worldSeed = seed;
    }

    public static long worldSeed() {
        return worldSeed;
    }

    public static final Codec<ShatteredSkyChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, ShatteredSkyChunkGenerator::new));

    public ShatteredSkyChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    // =====================================================================
    //  Island field (shared with the teleporter)
    // =====================================================================

    public record Island(int centerX, int centerZ, int radius, int baseY) {
    }

    /** Deterministic 0..1 hash per (cell, seed). */
    private static double hash01(int cellX, int cellZ, long seed, int salt) {
        long h = seed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return (h >>> 11) * 0x1.0p-53;
    }

    public static Island cellIsland(int cellX, int cellZ, long seed) {
        if (cellX == 0 && cellZ == 0) {
            return new Island(0, 0, 26, 100);
        }
        double presence = hash01(cellX, cellZ, seed, 1);
        if (presence < 0.18) {
            return null; // sparse gaps make the archipelago feel broken
        }
        int centerX = cellX * CELL_SIZE + (int) (hash01(cellX, cellZ, seed, 2) * (CELL_SIZE - 28)) + 14;
        int centerZ = cellZ * CELL_SIZE + (int) (hash01(cellX, cellZ, seed, 3) * (CELL_SIZE - 28)) + 14;
        int radius = 9 + (int) (hash01(cellX, cellZ, seed, 4) * 15);
        int baseY = 62 + (int) (hash01(cellX, cellZ, seed, 5) * 68);
        return new Island(centerX, centerZ, radius, baseY);
    }

    /** @return the surface Y of the tallest island at this column, or {@link #NO_SURFACE}. */
    public static int surfaceHeight(int x, int z, long seed) {
        int cellX = Math.floorDiv(x, CELL_SIZE);
        int cellZ = Math.floorDiv(z, CELL_SIZE);
        int best = NO_SURFACE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Island island = cellIsland(cellX + dx, cellZ + dz, seed);
                if (island == null) {
                    continue;
                }
                double distance = Math.sqrt(Math.pow(x - island.centerX, 2) + Math.pow(z - island.centerZ, 2));
                if (distance > island.radius) {
                    continue;
                }
                double falloff = 1.0 - (distance / island.radius);
                // Undulating plateau + a jagged rim that reads as shattered rock.
                double ripple = Math.sin(x * 0.21 + island.centerX) * Math.cos(z * 0.17 + island.centerZ) * 2.5;
                int surface = (int) Math.round(island.baseY + falloff * 9.0 + ripple);
                best = Math.max(best, surface);
            }
        }
        return best;
    }

    /** Nearest island centre to a point - used to place arrivals safely. */
    public static BlockPos nearestIslandCentre(double x, double z, long seed) {
        int cellX = Math.floorDiv((int) Math.floor(x), CELL_SIZE);
        int cellZ = Math.floorDiv((int) Math.floor(z), CELL_SIZE);
        Island best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Island island = cellIsland(cellX + dx, cellZ + dz, seed);
                if (island == null) {
                    continue;
                }
                double distance = Math.pow(x - island.centerX, 2) + Math.pow(z - island.centerZ, 2);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = island;
                }
            }
        }
        if (best == null) {
            return new BlockPos(0, 104, 0);
        }
        return new BlockPos(best.centerX, best.baseY + 8, best.centerZ);
    }

    // =====================================================================
    //  Chunk population
    // =====================================================================

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateIslands(chunk, worldSeed);
            return chunk;
        }, executor);
    }

    private void generateIslands(Chunk chunk, long seed) {
        ChunkPos chunkPos = chunk.getPos();
        BlockState stone = Blocks.STONE.getDefaultState();
        BlockState deepslate = Blocks.DEEPSLATE.getDefaultState();
        BlockState grass = ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getStartX() + localX;
                int worldZ = chunkPos.getStartZ() + localZ;
                int surface = surfaceHeight(worldX, worldZ, seed);
                if (surface == NO_SURFACE || surface <= MIN_Y + 2) {
                    continue;
                }
                int bottom = Math.max(MIN_Y + 1, surface - 20);
                for (int y = bottom; y <= surface; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (y == surface) {
                        chunk.setBlockState(pos, grass, false);
                    } else if (y < bottom + 3) {
                        chunk.setBlockState(pos, stone, false);
                    } else if (y < bottom + 12) {
                        chunk.setBlockState(pos, deepslate, false);
                    } else {
                        chunk.setBlockState(pos, stone, false);
                    }
                    // Aether-Blood veins in the deep core of larger islands.
                    if (y < bottom + 6 && ((worldX * 31 + y * 17 + worldZ * 13) & 63) == 0) {
                        chunk.setBlockState(pos, ModBlocks.AETHER_BLOOD_ORE.getDefaultState(), false);
                    }
                }
                // Ensure nothing floats beneath the island: clear a ragged underside.
                for (int y = bottom - 1; y > MIN_Y; y--) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (!chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, air, false);
                    }
                }
            }
        }
    }

    @Override
    public int getHeight(int x, int z, net.minecraft.world.Heightmap.Type heightmap,
                         HeightLimitView world, NoiseConfig noiseConfig) {
        if (heightmap == net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG
                || heightmap == net.minecraft.world.Heightmap.Type.OCEAN_FLOOR_WG) {
            return Math.max(world.getBottomY(), surfaceHeight(x, z, worldSeed()));
        }
        return Math.max(world.getBottomY(), surfaceHeight(x, z, worldSeed()) + 1);
    }

    @Override
    public net.minecraft.world.gen.chunk.VerticalBlockSample getColumnSample(
            int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottom = world.getBottomY();
        BlockState[] states = new BlockState[world.getHeight()];
        int surface = surfaceHeight(x, z, worldSeed());
        for (int i = 0; i < states.length; i++) {
            states[i] = bottom + i <= surface
                    ? ModBlocks.STAR_ASH.getDefaultState()
                    : Blocks.AIR.getDefaultState();
        }
        return new net.minecraft.world.gen.chunk.VerticalBlockSample(bottom, states);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // Surface is authored in generateIslands; nothing to smooth here.
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      net.minecraft.world.biome.source.BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, net.minecraft.world.gen.GenerationStep.Carver carverStep) {
        // No caves: the Shattered Sky's interiors are sealed ruins.
    }

    @Override
    public void getDebugHudText(java.util.List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Aetherius: Shattered Sky (island field, seed " + worldSeed() + ")");
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Spawns are handled by the biome spawner / structure features.
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        // Configured features for this dimension are placed by the biome's feature list.
        // Keeping this empty (instead of delegating) avoids double-placing island decor.
    }

    @Override
    public int getWorldHeight() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinimumY() {
        return MIN_Y;
    }

    @Override
    public int getSpawnHeight(HeightLimitView world) {
        return 104;
    }

    /** Slight altitude-based gravity compensation for procedural decoration helpers. */
    public static float ambientGravityFactor(int y) {
        return MathHelper.clamp(0.6f + (y / (float) HEIGHT) * 0.8f, 0.4f, 1.4f);
    }
}
