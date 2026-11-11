package com.aetherius.world;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldAccess;

/**
 * Spawn predicates for the Shattered Sky.
 *
 * <p>The dimension is mostly air: floating islands with open void between them. Mob spawn weights
 * alone do not care about that, and a type without a {@code SpawnRestriction} entry is treated as
 * "may spawn anywhere the position is free" — which is why specters and mantas appeared in the
 * middle of nowhere. Every entity of this mod is registered with one of the predicates below, using
 * vanilla's {@link net.minecraft.entity.SpawnRestriction} machinery (the same one cows and zombies
 * go through), so natural spawning respects the geometry.</p>
 */
public final class SkySpawnRules {

    /** How far below a spawn position we look for island ground before giving up. */
    public static final int ISLAND_SCAN_DEPTH = 24;

    private SkySpawnRules() {
    }

    /** Ground walkers: exactly like vanilla animals, feet must touch a solid block. */
    public static boolean onSolidGround(EntityType<?> type, ServerWorldAccess world,
                                        SpawnReason reason, BlockPos pos, Random random) {
        if (reason == SpawnReason.SPAWN_EGG || reason == SpawnReason.MOB_SUMMONED
                || reason == SpawnReason.COMMAND || reason == SpawnReason.BUCKET) {
            return true;
        }
        BlockPos below = pos.down();
        BlockState state = world.getBlockState(below);
        if (!state.isSolidBlock(world, below)) {
            return false;
        }
        return isFree(world, pos);
    }

    /**
     * Fliers: no ground contact required, but the spawn must be *near* an island so they do not
     * materialise in the open void. Also keeps a few blocks of headroom so they never clip inside
     * an island.
     */
    public static boolean nearIsland(EntityType<?> type, ServerWorldAccess world,
                                     SpawnReason reason, BlockPos pos, Random random) {
        if (reason == SpawnReason.SPAWN_EGG || reason == SpawnReason.MOB_SUMMONED
                || reason == SpawnReason.COMMAND || reason == SpawnReason.BUCKET) {
            return true;
        }
        if (!isFree(world, pos)) {
            return false;
        }
        // Headroom: an island above us counts as "inside the island", not "near it".
        for (int dy = 1; dy <= 3; dy++) {
            if (!world.getBlockState(pos.up(dy)).isAir()) {
                return false;
            }
        }
        return groundDistance(world, pos, ISLAND_SCAN_DEPTH) >= 0;
    }

    /** Sky bosses and anchors only make sense above an island, with a lot of air around them. */
    public static boolean openSkyOverIsland(EntityType<?> type, ServerWorldAccess world,
                                            SpawnReason reason, BlockPos pos, Random random) {
        if (reason == SpawnReason.SPAWN_EGG || reason == SpawnReason.MOB_SUMMONED
                || reason == SpawnReason.COMMAND) {
            return true;
        }
        if (!nearIsland(type, world, reason, pos, random)) {
            return false;
        }
        int floor = groundDistance(world, pos, ISLAND_SCAN_DEPTH);
        return floor >= 6;
    }

    /**
     * Success/failure spawns placed by the blight system still have to land on something solid,
     * otherwise abominations appear hovering over the void.
     */
    public static boolean blightSpawn(EntityType<?> type, ServerWorldAccess world,
                                      SpawnReason reason, BlockPos pos, Random random) {
        return onSolidGround(type, world, reason, pos, random);
    }

    /**
     * Vertical correction for hovering mobs, in blocks per tick. Keeps them in a band over the
     * island they belong to instead of drifting off into (or below) the void.
     */
    public static double hoverCorrection(WorldAccess world, BlockPos pos, double preferredAltitude) {
        int ground = groundDistance(world, pos, 96);
        if (ground < 0) {
            return 0.08;                       // open void: climb until we find an island
        }
        if (ground > preferredAltitude + 4) {
            return -0.06;
        }
        if (ground < preferredAltitude - 4) {
            return 0.06;
        }
        return 0.0;
    }

    /** True when the mob has fallen into the void and should be removed. */
    public static boolean isLostToVoid(WorldAccess world, BlockPos pos) {
        if (pos.getY() <= world.getBottomY() + 6) {
            return true;
        }
        return pos.getY() < world.getBottomY() + 24 && groundDistance(world, pos, 64) < 0;
    }

    /** Distance (in blocks) from {@code pos} down to the first solid block, or -1 if none. */
    public static int groundDistance(WorldAccess world, BlockPos pos, int maxDepth) {
        BlockPos.Mutable cursor = pos.mutableCopy();
        for (int dy = 0; dy < maxDepth; dy++) {
            cursor.setY(pos.getY() - dy);
            if (cursor.getY() < world.getBottomY()) {
                return -1;
            }
            BlockState state = world.getBlockState(cursor);
            if (state.isSolidBlock(world, cursor)) {
                return dy;
            }
        }
        return -1;
    }

    /** The spawn cell itself has to be breathable air with no fluid in it. */
    private static boolean isFree(ServerWorldAccess world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty()
                && world.getFluidState(pos).isEmpty()
                && pos.getY() > world.getBottomY() + 2;
    }

    /** Heightmap used by every restriction here. */
    public static final Heightmap.Type HEIGHTMAP = Heightmap.Type.MOTION_BLOCKING_NO_LEAVES;
}
