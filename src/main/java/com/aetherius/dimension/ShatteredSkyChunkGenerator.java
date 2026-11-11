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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Generator for <b>The Shattered Sky</b>: an end-like field of floating islands above an
 * endless void.
 *
 * <h2>Island field</h2>
 * A low-frequency value-noise field ({@link SkyNoise}) decides where solid rock exists: columns
 * whose noise clears {@link #ISLAND_THRESHOLD} belong to an island, and how far it clears the
 * threshold decides the island's <i>thickness</i>. That single number is what makes the shape read
 * as a floating rock: wide and flat at the rim, deep in the middle, with a tapered underside.
 * Altitude comes from a second, slower noise, so islands sit at different heights instead of one
 * flat layer - the same trick the End uses for its outer islands.
 *
 * <p>A guaranteed spawn island sits at the origin so the first portal trip, the teleporter's
 * fallback and the boss arena always have ground.</p>
 *
 * <h2>No water, no sea</h2>
 * The dimension has no sea level: columns without an island are pure void, and
 * {@link #surfaceHeight} answers {@link #NO_SURFACE} there. Every caller (teleporter, spawn rules,
 * heightmaps) treats that as "no ground here".
 *
 * <h2>Version note</h2>
 * The chunk-generator contract changes between Minecraft versions. If a mapping bump breaks
 * compilation, the volatile parts are {@code populateNoise}, {@code generateFeatures},
 * {@code buildSurface}, {@code getColumnSample} and {@code getDebugHudText} - not the island maths.
 */
public class ShatteredSkyChunkGenerator extends ChunkGenerator {

    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;
    /** Reported to vanilla; the dimension has no sea, this is only a hint for spawn placement. */
    public static final int SEA_LEVEL = 64;
    /** Nothing here: kept so callers can compare against a column with no island. */
    public static final int NO_SURFACE = Integer.MIN_VALUE;

    /** Noise above which a column is solid rock. */
    private static final double ISLAND_THRESHOLD = 0.56;
    /** Radius of the guaranteed spawn island. */
    public static final int SPAWN_RADIUS = 54;
    /** Altitude of the spawn island's surface. */
    public static final int SPAWN_HEIGHT = 92;

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
    //  Island field (shared with the teleporter, the biome regions and the boss arena)
    // =====================================================================

    /** Vertical extent of the island covering a column: {@code top} is the surface block. */
    public record Island(int top, int bottom) {
    }

    /**
     * The island at this column, or {@code null} for empty sky.
     *
     * <p>Pure and deterministic - chunk generation runs on worker threads.</p>
     */
    public static Island islandAt(int x, int z, long seed) {
        double distance = Math.hypot(x, z);
        if (distance < SPAWN_RADIUS) {
            // Spawn island: a wide, thick disc, deepest at the centre so it looks anchored.
            double t = 1.0 - distance / SPAWN_RADIUS;
            double rough = SkyNoise.fbm(x, z, seed ^ 0x5A17L, 0.03, 2);
            int top = SPAWN_HEIGHT + (int) Math.round((rough - 0.5) * 4.0);
            int bottom = top - (int) Math.round(10 + 20 * t * t);
            return new Island(top, bottom);
        }

        double field = SkyNoise.fbm(x, z, seed ^ 0x151A1DL, 0.0016, 4);
        double edge = SkyNoise.fbm(x, z, seed ^ 0x2E771L, 0.02, 2);
        double value = field + (edge - 0.5) * 0.06;
        if (value < ISLAND_THRESHOLD) {
            return null;
        }
        // 0 at the island rim, ~1 in its middle: drives both altitude and thickness.
        double t = Math.min(1.0, (value - ISLAND_THRESHOLD) / 0.44 * 1.8);
        double altitude = 58.0 + SkyNoise.fbm(x, z, seed ^ 0xA17L, 0.0007, 3) * 44.0;
        int top = (int) Math.round(MathHelper.clamp(altitude + t * 8.0 + (edge - 0.5) * 4.0,
                24.0, HEIGHT - 24.0));
        int bottom = top - (int) Math.round(6 + 34 * t);
        return new Island(top, Math.max(MIN_Y + 3, bottom));
    }

    /** @return the surface Y of the island at this column, or {@link #NO_SURFACE} for void. */
    public static int surfaceHeight(int x, int z, long seed) {
        Island island = islandAt(x, z, seed);
        return island == null ? NO_SURFACE : island.top();
    }

    /**
     * A safe arrival position: the column itself if it has ground, otherwise the nearest island
     * within {@link #SPAWN_RADIUS} blocks of search, otherwise the spawn island.
     */
    public static BlockPos nearestIslandCentre(double x, double z, long seed) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        Island here = islandAt(blockX, blockZ, seed);
        if (here != null) {
            return new BlockPos(blockX, here.top() + 2, blockZ);
        }
        for (int radius = 8; radius <= 96; radius += 8) {
            for (int step = 0; step < 16; step++) {
                double angle = (Math.PI * 2 / 16) * step;
                int sx = blockX + (int) Math.round(Math.cos(angle) * radius);
                int sz = blockZ + (int) Math.round(Math.sin(angle) * radius);
                Island island = islandAt(sx, sz, seed);
                if (island != null) {
                    return new BlockPos(sx, island.top() + 2, sz);
                }
            }
        }
        return new BlockPos(0, SPAWN_HEIGHT + 3, 0);
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
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState gravel = Blocks.GRAVEL.getDefaultState();
        BlockState coarseDirt = Blocks.COARSE_DIRT.getDefaultState();
        BlockState bedrock = Blocks.BEDROCK.getDefaultState();
        BlockState grass = ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState();
        BlockState ash = ModBlocks.STAR_ASH.getDefaultState();
        BlockState bloodOre = ModBlocks.AETHER_BLOOD_ORE.getDefaultState();
        BlockState veinOre = ModBlocks.STAR_VEIN_ORE.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getStartX() + localX;
                int worldZ = chunkPos.getStartZ() + localZ;
                Island island = islandAt(worldX, worldZ, seed);
                if (island == null) {
                    // Empty sky: clear anything a previously generated island left behind.
                    for (int y = MIN_Y; y < HEIGHT; y++) {
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        if (!chunk.getBlockState(pos).isAir()) {
                            chunk.setBlockState(pos, air, false);
                        }
                    }
                    continue;
                }

                ShatteredSkyRegion region = ShatteredSkyRegion.at(worldX, worldZ);
                BlockState top;
                BlockState filler = switch (region) {
                    case HIGHLANDS -> {
                        top = ash;
                        yield gravel;
                    }
                    case BARRENS -> {
                        top = ash;
                        yield coarseDirt;
                    }
                    default -> {
                        top = grass;
                        yield dirt;
                    }
                };

                int bottom = Math.max(MIN_Y + 1, island.bottom());
                int top2 = Math.min(HEIGHT - 1, island.top());
                for (int y = bottom; y <= top2; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state;
                    if (y == top2) {
                        state = top;
                    } else if (y >= top2 - 3) {
                        state = filler;
                    } else if (y < 32) {
                        state = deepslate;
                    } else {
                        state = stone;
                    }
                    if (state == stone || state == deepslate) {
                        int roll = oreRoll(worldX, y, worldZ, seed);
                        if (roll == 0 && y > 18) {
                            state = veinOre;
                        } else if (roll == 1 && y < 34) {
                            state = bloodOre;
                        }
                    }
                    chunk.setBlockState(pos, state, false);
                }
                // Bedrock floor of the island, so digging down never opens a hole into the void
                // that the player could not see coming.
                chunk.setBlockState(new BlockPos(worldX, bottom, worldZ), bedrock, false);
                // Anything above the surface must be air, or a taller neighbour island would
                // leave a cliff of stale blocks behind.
                for (int y = top2 + 1; y < HEIGHT; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (!chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, air, false);
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
        if (surface == NO_SURFACE) {
            return world.getBottomY() + 1;
        }
        boolean blockHeight = heightmap == Heightmap.Type.WORLD_SURFACE_WG
                || heightmap == Heightmap.Type.OCEAN_FLOOR_WG
                || heightmap == Heightmap.Type.WORLD_SURFACE
                || heightmap == Heightmap.Type.OCEAN_FLOOR;
        return Math.max(world.getBottomY(), blockHeight ? surface : surface + 1);
    }

    @Override
    public net.minecraft.world.gen.chunk.VerticalBlockSample getColumnSample(
            int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottom = world.getBottomY();
        Island island = islandAt(x, z, worldSeed);
        BlockState[] states = new BlockState[world.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = bottom + i;
            if (island == null || y > island.top() || y < island.bottom()) {
                states[i] = Blocks.AIR.getDefaultState();
            } else if (y == island.top()) {
                states[i] = ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState();
            } else {
                states[i] = Blocks.STONE.getDefaultState();
            }
        }
        return new net.minecraft.world.gen.chunk.VerticalBlockSample(bottom, states);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // The surface is authored in generateIslands; nothing to smooth here.
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      net.minecraft.world.biome.source.BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, net.minecraft.world.gen.GenerationStep.Carver carverStep) {
        // No carvers: the dimension ships an empty carver list.
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        Island island = islandAt(pos.getX(), pos.getZ(), worldSeed);
        text.add("Aetherius: Shattered Sky islands (seed " + worldSeed()
                + (island == null ? ", void column" : ", island " + island.bottom() + ".." + island.top()) + ")");
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

        // Stay a few blocks inside the chunk: a canopy is 2 wide, and writing outside the chunk
        // being generated is how neighbour chunks get corrupted.
        for (int localX = 3; localX < 13; localX++) {
            for (int localZ = 3; localZ < 13; localZ++) {
                int worldX = chunkPos.getStartX() + localX;
                int worldZ = chunkPos.getStartZ() + localZ;
                Island island = islandAt(worldX, worldZ, worldSeed);
                if (island == null) {
                    continue;
                }
                ShatteredSkyRegion region = ShatteredSkyRegion.at(worldX, worldZ);
                double roll = SkyNoise.hash(worldX, worldZ, worldSeed ^ 0xF00DL);
                BlockPos base = new BlockPos(worldX, island.top() + 1, worldZ);

                if (region == ShatteredSkyRegion.MEADOWS && roll < 0.02) {
                    placeOak(world, base, log, leaves, worldX, worldZ);
                } else if (region == ShatteredSkyRegion.HIGHLANDS && roll > 0.992) {
                    placeStandingStone(world, base, pillar);
                } else if (region == ShatteredSkyRegion.MEADOWS && roll > 0.96 && roll < 0.966) {
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
                        continue;
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
        return SPAWN_HEIGHT + 3;
    }

    /** Slight altitude-based gravity compensation for procedural decoration helpers. */
    public static float ambientGravityFactor(int y) {
        return MathHelper.clamp(0.6f + (y / (float) HEIGHT) * 0.8f, 0.4f, 1.4f);
    }
}
