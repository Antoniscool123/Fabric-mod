package com.aetherius.dimension;

import com.aetherius.registry.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Temporal storms: the Shattered Sky's weather.
 *
 * <p>Every few minutes the dimension's time-fracture rolls over the island the player is
 * standing on: lightning, particle wind, slowness for exposed players and a gravity wobble.
 * Ties directly into the "temporal storms" pillar of Act III and gives the final approach to
 * Nox'Thal a hostile, unstable feel.</p>
 */
public final class TemporalStormManager {

    private static final int MIN_INTERVAL = 20 * 90;
    private static final int MAX_INTERVAL = 20 * 240;
    private static final int STORM_DURATION = 20 * 40;

    private long nextStormTick;
    private long stormEndTick;
    private boolean announced;

    public void tick(MinecraftServer server) {
        ServerWorld sky = server.getWorld(AetherDimensions.SHATTERED_SKY);
        if (sky == null) {
            return;
        }
        long now = sky.getTime();
        if (nextStormTick == 0) {
            nextStormTick = now + MIN_INTERVAL;
            return;
        }
        if (now >= nextStormTick && now >= stormEndTick) {
            stormEndTick = now + STORM_DURATION;
            nextStormTick = stormEndTick + MIN_INTERVAL
                    + sky.getRandom().nextInt(MAX_INTERVAL - MIN_INTERVAL);
            announced = false;
        }
        if (now < stormEndTick) {
            driveStorm(sky, now);
        }
    }

    private void driveStorm(ServerWorld sky, long now) {
        for (ServerPlayerEntity player : sky.getPlayers()) {
            if (!announced) {
                player.sendMessage(Text.translatable("aetherius.storm.begin")
                        .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC), false);
            }
            if (now % 10 == 0) {
                sky.spawnParticles(ModParticles.STAR_DUST,
                        player.getX(), player.getY() + 3, player.getZ(), 24, 6.0, 3.0, 6.0, 0.4);
            }
            if (now % 60 == 0) {
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.SLOWNESS, 60, 0, false, false, false));
                player.addVelocity((sky.getRandom().nextDouble() - 0.5) * 0.12, 0.04, 
                        (sky.getRandom().nextDouble() - 0.5) * 0.12);
                player.velocityModified = true;
            }
            if (now % 100 == 0 && sky.getRandom().nextFloat() < 0.4f) {
                ChunkPos chunk = new ChunkPos(player.getBlockPos());
                BlockPos strike = sky.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
                        chunk.getStartPos().add(sky.getRandom().nextInt(15), 0, sky.getRandom().nextInt(15)));
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(sky);
                if (bolt != null) {
                    bolt.refreshPositionAfterTeleport(net.minecraft.util.math.Vec3d.ofBottomCenter(strike));
                    sky.spawnEntity(bolt);
                }
            }
        }
        if (!announced) {
            announced = true;
            for (ServerPlayerEntity player : sky.getPlayers()) {
                player.sendMessage(Text.translatable("aetherius.storm.announce").formatted(Formatting.LIGHT_PURPLE),
                        false);
            }
        }
    }

    public boolean isStormActive(ServerWorld world) {
        return world.getTime() < stormEndTick;
    }
}
