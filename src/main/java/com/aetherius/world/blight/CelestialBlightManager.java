package com.aetherius.world.blight;

import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.registry.ModBlocks;
import com.aetherius.registry.ModEntities;
import com.aetherius.registry.ModParticles;
import com.aetherius.world.AetheriusWorldState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aspect Corruption: Celestial Blight.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Blight lives per chunk with an intensity of 0-100.</li>
 *   <li>Above 20 intensity the chunk starts converting grass to Star-Ash.</li>
 *   <li>Above 40 intensity passive mobs are replaced by Void-Warped Abominations.</li>
 *   <li>Above 60 intensity the chunk can spread to orthogonal neighbours.</li>
 *   <li>The Aetherial Altar's rite clears every chunk within radius instantly.</li>
 * </ul>
 *
 * <p>All block edits are budgeted per tick so a heavily blighted region cannot stall the
 * server: each pass touches at most {@link #BLOCK_BUDGET} blocks per world tick.</p>
 */
public final class CelestialBlightManager {

    private static final CelestialBlightManager INSTANCE = new CelestialBlightManager();

    public static final int BLOCK_BUDGET = 24;
    public static final int SPREAD_INTERVAL_TICKS = 100;
    public static final int ABOMINATION_THRESHOLD = 40;
    public static final int SPREAD_THRESHOLD = 60;

    private int spreadTimer;

    private CelestialBlightManager() {
    }

    public static CelestialBlightManager get() {
        return INSTANCE;
    }

    // =====================================================================
    //  Queries
    // =====================================================================

    public boolean isChunkBlighted(ServerWorld world, BlockPos pos) {
        return isChunkBlighted(world, new ChunkPos(pos));
    }

    public boolean isChunkBlighted(ServerWorld world, ChunkPos pos) {
        return AetheriusWorldState.get(world).blightAt(pos) > 0;
    }

    public int intensity(ServerWorld world, BlockPos pos) {
        return AetheriusWorldState.get(world).blightAt(new ChunkPos(pos));
    }

    // =====================================================================
    //  Infection
    // =====================================================================

    /**
     * Seeds blight in the chunk containing {@code origin}.
     *
     * @param strength intensity added (0-100); a backfire adds ~30
     */
    public void infect(ServerWorld world, BlockPos origin, int strength) {
        AetheriusWorldState state = AetheriusWorldState.get(world);
        ChunkPos chunk = new ChunkPos(origin);
        int intensity = state.addBlight(chunk, strength);
        notifyNearby(world, origin, Text.translatable("aetherius.blight.seeded",
                Text.literal(String.valueOf(intensity)).formatted(Formatting.DARK_PURPLE)));

        Random random = world.getRandom();
        convertArea(world, origin, 6, random);
        if (intensity >= ABOMINATION_THRESHOLD) {
            warpPassiveMobs(world, origin, 24);
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getBlockPos().isWithinDistance(origin, 24)
                    && player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD)
                    .getItem() instanceof com.aetherius.item.AethersightMonocleItem) {
                ChronicleManager.get().onBlightWitnessed(player);
            }
        }
    }

    private void convertArea(ServerWorld world, BlockPos center, int radius, Random random) {
        int budget = BLOCK_BUDGET;
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius / 2 + 1, radius)) {
            if (budget-- <= 0) {
                return;
            }
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(ModBlocks.STARLIT_GRASS_BLOCK)) {
                world.setBlockState(pos, ModBlocks.STAR_ASH.getDefaultState());
            } else if (state.isIn(BlockTags.LEAVES) && random.nextFloat() < 0.5f) {
                world.breakBlock(pos, false);
            } else if (state.isIn(BlockTags.FLOWERS) && random.nextFloat() < 0.8f) {
                world.breakBlock(pos, false);
            }
            if (random.nextInt(24) == 0) {
                world.spawnParticles(ModParticles.BLIGHT_SPORE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    private void warpPassiveMobs(ServerWorld world, BlockPos center, int radius) {
        for (PassiveEntity passive : world.getEntitiesByClass(PassiveEntity.class,
                new net.minecraft.util.math.Box(center, center).expand(radius), PassiveEntity::isAlive)) {
            if (world.random.nextFloat() > 0.25f) {
                continue;
            }
            BlockPos spawnPos = passive.getBlockPos();
            passive.discard();
            EntityType<?> type = ModEntities.VOID_WARPED_ABOMINATION;
            var abomination = type.create(world);
            if (abomination == null) {
                continue;
            }
            abomination.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    world.random.nextFloat() * 360f, 0f);
            if (abomination instanceof HostileEntity hostile) {
                hostile.setPersistent();
            }
            world.spawnEntity(abomination);
            world.spawnParticles(ModParticles.BLIGHT_SPORE,
                    spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 30, 0.6, 0.8, 0.6, 0.06);
        }
    }

    // =====================================================================
    //  Spread / decay (called once per server tick)
    // =====================================================================

    public void tick(ServerWorld world) {
        if (++spreadTimer < SPREAD_INTERVAL_TICKS) {
            return;
        }
        spreadTimer = 0;
        AetheriusWorldState state = AetheriusWorldState.get(world);
        Map<Long, Integer> snapshot = state.blightSnapshot();
        Random random = world.getRandom();
        List<long[]> newInfections = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : snapshot.entrySet()) {
            long chunkKey = entry.getKey();
            int intensity = entry.getValue();
            if (intensity < SPREAD_THRESHOLD || random.nextFloat() > 0.12f) {
                continue;
            }
            int cx = ChunkPos.getPackedX(chunkKey);
            int cz = ChunkPos.getPackedZ(chunkKey);
            int[][] neighbours = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            int[] offset = neighbours[random.nextInt(neighbours.length)];
            newInfections.add(new long[]{ChunkPos.toLong(cx + offset[0], cz + offset[1]), 12L});
        }
        for (long[] infection : newInfections) {
            state.addBlight(new ChunkPos(infection[0]), (int) infection[1]);
        }

        // Ambient decay: blight slowly burns itself out below the spread threshold.
        for (Map.Entry<Long, Integer> entry : snapshot.entrySet()) {
            int intensity = entry.getValue();
            if (intensity > 0 && intensity < SPREAD_THRESHOLD && random.nextFloat() < 0.05f) {
                state.setBlight(new ChunkPos(entry.getKey()), intensity - 1);
            }
        }
    }

    // =====================================================================
    //  Terrain reactions
    // =====================================================================

    /** Called from {@code StarAshBlock#randomTick}. */
    public void tickStarAsh(ServerWorld world, BlockPos pos, Random random) {
        boolean blighted = isChunkBlighted(world, pos);
        if (!blighted) {
            if (random.nextFloat() < 0.15f) {
                world.setBlockState(pos, ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState());
            }
            return;
        }
        if (random.nextFloat() < 0.12f) {
            BlockPos above = pos.up();
            if (world.isAir(above)) {
                world.setBlockState(above, Blocks.SOUL_FIRE.getDefaultState());
            }
        }
        if (random.nextFloat() < 0.05f) {
            BlockPos side = pos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (world.getBlockState(side).isOf(Blocks.GRASS_BLOCK)) {
                world.setBlockState(side, ModBlocks.STAR_ASH.getDefaultState());
            }
        }
    }

    // =====================================================================
    //  Purification
    // =====================================================================

    /**
     * Clears blight in every chunk within {@code radius} blocks and reverts surface Star-Ash.
     *
     * @return number of chunks purified
     */
    public int purify(ServerWorld world, BlockPos center, int radius) {
        AetheriusWorldState state = AetheriusWorldState.get(world);
        int chunkRadius = Math.max(1, radius / 16);
        int cleared = state.clearBlightAround(new ChunkPos(center), chunkRadius);

        int budget = 900;
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, 32, radius)) {
            if (budget-- <= 0) {
                break;
            }
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isOf(ModBlocks.STAR_ASH)) {
                world.setBlockState(pos, ModBlocks.STARLIT_GRASS_BLOCK.getDefaultState());
            } else if (blockState.isOf(Blocks.SOUL_FIRE)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }
        // Fade and normalise abominations: they lose aggression and can be killed cleanly.
        for (var abomination : world.getEntitiesByClass(
                com.aetherius.entity.VoidWarpedAbominationEntity.class,
                new net.minecraft.util.math.Box(center, center).expand(radius), e -> e.isAlive())) {
            if (world.random.nextFloat() < 0.5f) {
                abomination.discard();
                world.spawnParticles(ModParticles.STAR_DUST, abomination.getX(),
                        abomination.getBodyY(0.5), abomination.getZ(), 40, 0.8, 1.0, 0.8, 0.1);
            }
        }
        notifyNearby(world, center, Text.translatable("aetherius.blight.purified",
                Text.literal(String.valueOf(cleared)).formatted(Formatting.AQUA)));
        return cleared;
    }

    private void notifyNearby(ServerWorld world, BlockPos center, Text message) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getBlockPos().isWithinDistance(center, 64)) {
                player.sendMessage(message, true);
            }
        }
    }

    /** Helper used by the debug command and the HUD compass. */
    public int topSurfaceY(ServerWorld world, BlockPos pos) {
        return world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }
}
