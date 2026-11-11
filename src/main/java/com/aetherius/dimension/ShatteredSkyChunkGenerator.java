package com.aetherius.dimension;

import com.aetherius.registry.ModBlocks;
import com.aetherius.worldgen.ShatteredSkyRegion;
import com.aetherius.worldgen.SkyNoise;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Generator for <b>The Shattered Sky</b>: ordinary, walkable terrain with water, beaches, hills
 * and three Aetherius biomes.
 *
 * <h2>Terrain</h2>
 * A column's height is a sum of three value-noise octaves ({@link SkyNoise}): a slow continental
 * field, a hill field and a fine detail field, offset so the average surface sits a few blocks
 * above {@link #SEA_LEVEL}. Low ground floods, so the dimension has real coastlines instead of the
 * endless island field it used to have. A plateau is forced around the origin so portal arrivals
 * (and the arena) always land on solid, flat ground.
 *
 * <h2>Biomes</h2>
 * {@link ShatteredSkyRegion#at(int, int)} chooses the region from the same noise, and this class
 * uses it for the surface blocks too - which is why a biome always matches the ground under it.
 * The biome source is {@code aetherius:shattered_sky} ({@code ShatteredSkyBiomeSource}).
 *
 * <h2>Decoration</h2>
 * {@link #generateFeatures} plants oaks in the meadows, standing stones in the highlands and mana
 * lilies near water. It only ever writes inside the chunk it is given, so it can never touch a
 * neighbouring chunk that has not been generated yet.
 *
 * <h2>Version note</h2>
 * The chunk-generator contract changes between Minecraft versions. If a mapping bump breaks
 * compilation, the volatile parts are {@code populateNoise}, {@code generateFeatures},
 * {@code buildSurface} and {@code getColumnSample} - not the height maths.
 */
public class ShatteredSkyChunkGenerator extends ChunkGenerator {

    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;
    public static final int SEA_LEVEL = 76;
    /** Kept for callers that ask "is there ground here?" - this generator always answers. */
    public static final int NO_SURFACE = Integer.MIN_VALUE;
    /** Radius of the flat spawn plateau around the origin. */
    private static final int SPAWN_PLATEAU = 72;
    private static final int SPAWN_PLATEAU_HEIGHT = 92;

    /** World seed, pushed in by the mod initialiser so terrain is deterministic per world. */
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
    //  Height field (shared with the teleporter, the biome regions and the boss arena)
    // =====================================================================

    /**
     * Surface height of a column, ignoring water.
     *
     * @return the Y of the top solid block; never below {@link #MIN_Y}+1
     */
    public static int surfaceHeight(int x, int z, long seed) {
        double continent = SkyNoise.fbm(x, z, seed ^ 0xA17L, 0.0035, 4);
        double hills = SkyNoise.fbm(x, z, seed ^ 0xB29L, 0.012, 3);
        double detail = SkyNoise.fbm(x, z, seed ^ 0xC33L, 0.05, 2);
        double height = 46.0 + continent * 52.0 + hills * 16.0 + detail * 5.0;

        // Flat spawn plateau: a smooth bowl-free bump so the first teleport always lands well.
        double distance = Math.hypot(x, z);
        if (distance < SPAWN_PLATEAU) {
            double lift = 1.0 - distance / SPAWN_PLATEAU;
            height = Math.max(height, 56.0 + (SPAWN_PLATEAU_HEIGHT - 56.0) * lift * lift);
        }
        return (int) Math.round(MathHelper.clamp(height, SEA_LEVEL - 24.0, HEIGHT - 12.0));
    }

    /** Solid, dry arrival position above a column; used by the teleporter's fallback. */
    public static BlockPos nearestIslandCentre(double x, double z, long seed) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int surface = Math.max(surfaceHeight(blockX, blockZ, seed), SEA_LEVEL) + 2;
        return new BlockPos(blockX, surface, blockZ);
    }

    // =====================================================================
    //  Chunk population
    // =====================================================================

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateTerrain(chunk, worldSeed);
            return chunk;
        }, executor);
    }

    private void generateTerrain(Chunk chunk, long seed) {
        ChunkPos chunkPos = chunk.getPos();
        BlockState stone = Blocks.STONE.getDefaultState();
        BlockState deepslate = Blocks.DEEPSLATE.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState sand = Blocks.SAND.getDefaultState();
        BlockState gravel = Blocks.GRAVEL.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState bedrock = Blocks.BEDROCK.getDefaultState();
        BlockState grass = ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState();
        BlockState ash = ModBlocks.STAR_ASH.getDefaultState();
        BlockState coarseDirt = Blocks.COARSE_DIRT.getDefaultState();
        BlockState bloodOre = ModBlocks.AETHER_BLOOD_ORE.getDefaultState();
        BlockState veinOre = ModBlocks.STAR_VEIN_ORE.getDefaultState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getStartX() + localX;
                int worldZ = chunkPos.getStartZ() + localZ;
                int surface = surfaceHeight(worldX, worldZ, seed);
                ShatteredSkyRegion region = ShatteredSkyRegion.at(worldX, worldZ);
                boolean shore = surface <= SEA_LEVEL + 2;

                // Top block: sand on the shoreline, otherwise the biome's own surface.
                BlockState top;
                BlockState filler;
                if (shore) {
                    top = sand;
                    filler = sand;
                } else {
                    switch (region) {
                        case HIGHLANDS -> {
                            top = ash;
                            filler = gravel;
                        }
                        case BARRENS -> {
                            top = ash;
                            filler = coarseDirt;
                        }
                        default -> {
                            top = grass;
                            filler = dirt;
                        }
                    }
                }

                chunk.setBlockState(new BlockPos(worldX, MIN_Y, worldZ), bedrock, false);
                for (int y = MIN_Y + 1; y <= surface; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state;
                    if (y == surface) {
                        state = top;
                    } else if (y >= surface - 3) {
                        state = filler;
                    } else if (y < 32) {
                        state = deepslate;
                    } else {
                        state = stone;
                    }
                    // Ores, keyed off a coordinate hash so a vein is reproducible from the seed.
                    if (state == stone || state == deepslate) {
                        int roll = oreRoll(worldX, y, worldZ, seed);
                        if (roll == 0 && y > 18 && y < surface - 6) {
                            state = veinOre;
                        } else if (roll == 1 && y < 34) {
                            state = bloodOre;
                        }
                    }
                    chunk.setBlockState(pos, state, false);
                }

                // Water, and the sea floor beneath it.
                if (surface < SEA_LEVEL) {
                    for (int y = surface + 1; y <= SEA_LEVEL; y++) {
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), water, false);
                    }
                }
            }
        }
    }

    /** 0 = star-vein, 1 = aether-blood, -1 = plain stone. Roughly 1 vein block in 230. */
    private static int oreRoll(int x, int y, int z, long seed) {
        double roll = SkyNoise.hash(x * 7 + z * 13, y * 31, seed ^ 0x0A17EL);
        if (roll < 0.0022) {
            return 0;
        }
        return roll > 0.9978 ? 1 : -1;
    }

    // =====================================================================
    //  Queries used by vanilla and by our own code
    // =====================================================================

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        int surface = surfaceHeight(x, z, worldSeed);
        int top = Math.max(surface, SEA_LEVEL);
        boolean surfaceType = heightmap == Heightmap.Type.WORLD_SURFACE_WG
                || heightmap == Heightmap.Type.OCEAN_FLOOR_WG
                || heightmap == Heightmap.Type.WORLD_SURFACE
                || heightmap == Heightmap.Type.OCEAN_FLOOR;
        // MOTION_BLOCKING and friends want the first *free* Y, i.e. one above the top block.
        return Math.max(world.getBottomY(), surfaceType ? top : top + 1);
    }

    @Override
    public net.minecraft.world.gen.chunk.VerticalBlockSample getColumnSample(
            int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottom = world.getBottomY();
        int surface = surfaceHeight(x, z, worldSeed);
        BlockState[] states = new BlockState[world.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = bottom + i;
            if (y < MIN_Y) {
                states[i] = Blocks.VOID_AIR.getDefaultState();
            } else if (y == MIN_Y) {
                states[i] = Blocks.BEDROCK.getDefaultState();
            } else if (y <= surface) {
                states[i] = y == surface
                        ? ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState()
                        : Blocks.STONE.getDefaultState();
            } else if (y <= SEA_LEVEL) {
                states[i] = Blocks.WATER.getDefaultState();
            } else {
                states[i] = Blocks.AIR.getDefaultState();
            }
        }
        return new net.minecraft.world.gen.chunk.VerticalBlockSample(bottom, states);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // The surface is authored in generateTerrain; nothing to smooth here.
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      net.minecraft.world.biome.source.BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, net.minecraft.world.gen.GenerationStep.Carver carverStep) {
        // No carvers: the dimension ships an empty carver list, so caves are simply not carved.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Spawns are handled by the biome spawners and the spawn restrictions.
    }

    // =====================================================================
    //  Decoration (trees, standing stones, lilies)
    // =====================================================================

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        ChunkPos chunkPos = chunk.getPos();
        BlockState log = Blocks.OAK_LOG.getDefaultState();
        BlockState leaves = Blocks.OAK_LEAVES.getDefaultState();
        BlockState pillar = ModBlocks.CELESTIAL_PILLAR.getDefaultState();
        BlockState lily = ModBlocks.MANA_LILY.getDefaultState();

        // Stay a few blocks inside the chunk: a tree's canopy is 2 wide, and writing outside the
        // chunk that is currently being generated is how neighbour chunks get corrupted.
        for (int localX = 3; localX < 13; localX++) {
            for (int localZ = 3; localZ < 13; localZ++) {
                int worldX = chunkPos.getStartX() + localX;
                int worldZ = chunkPos.getStartZ() + localZ;
                int surface = surfaceHeight(worldX, worldZ, worldSeed);
                if (surface < SEA_LEVEL + 1) {
                    continue;
                }
                ShatteredSkyRegion region = ShatteredSkyRegion.at(worldX, worldZ);
                double roll = SkyNoise.hash(worldX, worldZ, worldSeed ^ 0xF00DL);
                BlockPos base = new BlockPos(worldX, surface + 1, worldZ);

                if (region == ShatteredSkyRegion.MEADOWS && roll < 0.014) {
                    placeOak(world, base, log, leaves, worldX, worldZ);
                } else if (region == ShatteredSkyRegion.HIGHLANDS && roll > 0.995) {
                    placeStandingStone(world, base, pillar);
                } else if (region == ShatteredSkyRegion.MEADOWS && roll > 0.972 && roll < 0.976) {
                    if (world.getBlockState(base.down()).isOf(ModBlocks.STARLIT_GRASS_BLOCK)) {
                        world.setBlockState(base, lily, 2);
                    }
                }
            }
        }
    }

    private void placeOak(StructureWorldAccess world, BlockPos base, BlockState log, BlockState leaves,
                          int worldX, int worldZ) {
        int height = 4 + (int) (SkyNoise.hash(worldX, worldZ, 0x7EEE5L) * 3.0);
        for (int i = 0; i < height; i++) {
            world.setBlockState(base.up(i), log, 2);
        }
        BlockPos crown = base.up(height);
        for (int dy = -2; dy <= 1; dy++) {
            int radius = dy >= 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy >= 0) {
                        continue; // trimmed corners read better than a cube
                    }
                    BlockPos leaf = crown.add(dx, dy, dz);
                    if (world.getBlockState(leaf).isAir()) {
                        world.setBlockState(leaf, leaves, 2);
                    }
                }
            }
        }
    }

    private void placeStandingStone(StructureWorldAccess world, BlockPos base, BlockState pillar) {
        int height = 2 + (int) (SkyNoise.hash(base.getX(), base.getZ(), 0x5717EL) * 3.0);
        for (int i = 0; i < height; i++) {
            BlockPos pos = base.up(i);
            if (world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, pillar, 2);
            }
        }
    }

    // =====================================================================
    //  Shape
    // =====================================================================

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
        return Math.max(surfaceHeight(0, 0, worldSeed) + 2, SEA_LEVEL + 3);
    }

    /** Slight altitude-based gravity compensation for procedural decoration helpers. */
    public static float ambientGravityFactor(int y) {
        return MathHelper.clamp(0.6f + (y / (float) HEIGHT) * 0.8f, 0.4f, 1.4f);
    }
}
