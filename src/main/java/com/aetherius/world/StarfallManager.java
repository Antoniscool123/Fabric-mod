package com.aetherius.world;

import com.aetherius.registry.ModBlocks;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Localized Star-fall events.
 *
 * <p>Breaking Star-Vein Ore calls {@link #triggerStarfall(ServerWorld, BlockPos, int)}, which
 * queues a handful of meteor impacts over the next 3 seconds. Impacts crater the surface,
 * seed Star-Ash and drop a scatter of Aether Shards, giving the Act I trigger real
 * spectacle instead of a particle puff.</p>
 *
 * <p>Events are queued with absolute tick timestamps so they survive a lag spike without
 * silently dropping meteors.</p>
 */
public final class StarfallManager {

    private static final StarfallManager INSTANCE = new StarfallManager();

    private final List<PendingImpact> impacts = new ArrayList<>();
    private long lastStarfallTick = Long.MIN_VALUE;

    private StarfallManager() {
    }

    public static StarfallManager get() {
        return INSTANCE;
    }

    public void triggerStarfall(ServerWorld world, BlockPos origin, int radius) {
        long now = world.getTime();
        if (now - lastStarfallTick < 200) {
            return; // anti-spam: one star-fall per 10 seconds per world
        }
        lastStarfallTick = now;

        int meteors = 4 + world.random.nextInt(4);
        for (int i = 0; i < meteors; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = 6 + world.random.nextDouble() * radius;
            BlockPos target = BlockPos.ofFloored(origin.getX() + Math.cos(angle) * distance,
                    origin.getY(), origin.getZ() + Math.sin(angle) * distance);
            impacts.add(new PendingImpact(world, target, now + 10 + world.random.nextInt(50)));
        }

        world.playSound(null, origin, ModSounds.STARFALL, SoundCategory.WEATHER, 2.0f, 0.8f);
        for (var player : world.getPlayers()) {
            if (player.getBlockPos().isWithinDistance(origin, radius * 2.0)) {
                player.sendMessage(Text.translatable("aetherius.starfall.beginning")
                        .formatted(Formatting.AQUA, Formatting.ITALIC), false);
            }
        }
    }

    /** Called from the server tick event. */
    public void tick(MinecraftServer server) {
        if (impacts.isEmpty()) {
            return;
        }
        Iterator<PendingImpact> iterator = impacts.iterator();
        while (iterator.hasNext()) {
            PendingImpact impact = iterator.next();
            if (impact.world.getTime() < impact.resolveAt) {
                // Draw the descending meteor trail while waiting.
                Vec3d pos = Vec3d.ofBottomCenter(impact.target).add(0, 26, 0);
                impact.world.spawnParticles(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, 4, 0.3, 0.3, 0.3, 0.05);
                continue;
            }
            iterator.remove();
            resolve(impact);
        }
    }

    private void resolve(PendingImpact impact) {
        BlockPos surface = impact.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, impact.target);
        impact.world.createExplosion(null, surface.getX() + 0.5, surface.getY() + 0.5, surface.getZ() + 0.5,
                1.6f, net.minecraft.world.World.ExplosionSourceType.BLOCK);
        impact.world.spawnParticles(ModParticles.STAR_DUST,
                surface.getX() + 0.5, surface.getY() + 1, surface.getZ() + 0.5, 90, 2.0, 1.5, 2.0, 0.3);
        impact.world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                surface.getX() + 0.5, surface.getY() + 1, surface.getZ() + 0.5, 2, 0.6, 0.6, 0.6, 0.0);
        impact.world.playSound(null, surface, ModSounds.STARFALL, SoundCategory.WEATHER, 1.8f, 1.1f);

        // A scorched landing zone: ash rim + a small shard scatter.
        for (BlockPos pos : BlockPos.iterateOutwards(surface, 3, 1, 3)) {
            if (impact.world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK)
                    || impact.world.getBlockState(pos).isOf(ModBlocks.STARLIT_GRASS_BLOCK)) {
                impact.world.setBlockState(pos, ModBlocks.STAR_ASH.getDefaultState());
            }
        }
        if (impact.world.random.nextFloat() < 0.6f) {
            impact.world.setBlockState(surface, ModBlocks.STAR_VEIN_ORE.getDefaultState());
        }
    }

    private record PendingImpact(ServerWorld world, BlockPos target, long resolveAt) {
    }
}
