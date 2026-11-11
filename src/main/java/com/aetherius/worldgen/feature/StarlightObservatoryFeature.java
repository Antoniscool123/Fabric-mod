package com.aetherius.worldgen.feature;

import com.aetherius.registry.ModBlocks;
import com.aetherius.world.AetheriusWorldState;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * <b>The Starlight Observatory</b> - the natural structure of Act I.
 *
 * <p>Generated procedurally (no structure NBT) so it can adapt to terrain: a quartz platform
 * with a lapis inlay, four shattered Celestial Pillars, a broken lintel, two rune lanterns and
 * the <i>Telescope of the Ancients</i> at the centre. The feature writes its own position to
 * {@link AetheriusWorldState} so the Astrolabe of the Void can point at the nearest one
 * without an expensive structure-locator search.</p>
 *
 * <p>In a production build this would likely ship as a jigsaw structure with loot tables and
 * a terrain-adaptation processor; the feature approach is used here because it needs zero
 * binary assets and is fully deterministic.</p>
 */
public class StarlightObservatoryFeature extends Feature<DefaultFeatureConfig> {

    private static final int PLATFORM_RADIUS = 6;

    public StarlightObservatoryFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        BlockPos surface = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, origin);
        if (surface.getY() < world.getBottomY() + 8 || surface.getY() > world.getTopY() - 8) {
            return false;
        }

        BlockPos base = surface.down();
        buildPlatform(world, base, random);
        buildPillars(world, base, random);
        buildTelescope(world, base);
        buildDebris(world, base, random);

        if (world instanceof ServerWorld serverWorld) {
            AetheriusWorldState.get(serverWorld).addObservatory(base.up());
        }
        return true;
    }

    private void buildPlatform(StructureWorldAccess world, BlockPos base, Random random) {
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > PLATFORM_RADIUS) {
                    continue;
                }
                BlockPos floor = base.add(dx, 0, dz);
                boolean rim = distance > PLATFORM_RADIUS - 1.2;
                BlockStateChoice choice = rim
                        ? new BlockStateChoice(Blocks.QUARTZ_BRICKS, 3, Blocks.CHISELED_QUARTZ_BLOCK, 1)
                        : new BlockStateChoice(Blocks.SMOOTH_QUARTZ, 5, Blocks.LAPIS_BLOCK, 1);
                setBlockState(world, floor, choice.pick(random));
                // Clear the interior volume so the telescope is actually usable.
                for (int dy = 1; dy <= 4; dy++) {
                    setBlockState(world, floor.up(dy), Blocks.AIR);
                }
                if (distance < 1.5) {
                    // Inlaid star pattern beneath the telescope.
                    setBlockState(world, floor, Blocks.LAPIS_BLOCK);
                }
            }
        }
    }

    private void buildPillars(StructureWorldAccess world, BlockPos base, Random random) {
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction direction : directions) {
            BlockPos pillarBase = base.offset(direction, 5);
            int height = 2 + random.nextInt(3); // deliberately uneven: the temple is ruined
            for (int dy = 1; dy <= height; dy++) {
                setBlockState(world, pillarBase.up(dy), ModBlocks.CELESTIAL_PILLAR);
            }
            if (random.nextBoolean()) {
                setBlockState(world, pillarBase.up(height + 1), Blocks.CRACKED_DEEPSLATE_BRICKS);
            }
            // Half the pillars carry a lantern stub - Phase 3 navigation landmarks.
            if (random.nextFloat() < 0.5f) {
                setBlockState(world, pillarBase, ModBlocks.RUNE_LANTERN);
            }
        }
    }

    private void buildTelescope(StructureWorldAccess world, BlockPos base) {
        setBlockState(world, base.up(), ModBlocks.CELESTIAL_PILLAR);
        setBlockState(world, base.up(2), ModBlocks.TELESCOPE);
        // A cracked arch over the instrument.
        for (int dx = -2; dx <= 2; dx++) {
            if (dx == 0) {
                continue;
            }
            setBlockState(world, base.up(5).add(dx, 0, -3), Blocks.CRACKED_DEEPSLATE_BRICKS);
        }
    }

    private void buildDebris(StructureWorldAccess world, BlockPos base, Random random) {
        for (int i = 0; i < 10; i++) {
            int dx = random.nextInt(PLATFORM_RADIUS * 2 + 1) - PLATFORM_RADIUS;
            int dz = random.nextInt(PLATFORM_RADIUS * 2 + 1) - PLATFORM_RADIUS;
            BlockPos pos = base.add(dx, 1, dz);
            if (world.getBlockState(pos).isAir()) {
                setBlockState(world, pos, random.nextBoolean()
                        ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_TILES);
            }
        }
    }

    private void setBlockState(StructureWorldAccess world, BlockPos pos, Block block) {
        world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private record BlockStateChoice(Block primary, int primaryWeight, Block secondary, int secondaryWeight) {
        Block pick(Random random) {
            return random.nextInt(primaryWeight + secondaryWeight) < primaryWeight ? primary : secondary;
        }
    }
}
