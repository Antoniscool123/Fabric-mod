package com.aetherius.dimension;

import com.aetherius.registry.ModSounds;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Moves players between the Overworld and the Shattered Sky.
 *
 * <p>Arrivals are offset to a safe column above the target chunk's island, using the same
 * deterministic island function as the chunk generator (see
 * {@link ShatteredSkyChunkGenerator#surfaceHeight(int, int, long)}). Return trips land on the
 * player's respawn point when one is set, otherwise the overworld spawn.</p>
 */
public final class SkyRiftTeleporter {

    private static final int PORTAL_COOLDOWN = 120;

    private SkyRiftTeleporter() {
    }

    public static void travel(ServerPlayerEntity player) {
        if (player.getWorld().isClient()) {
            return;
        }
        ServerWorld current = player.getServerWorld();
        boolean inSky = current.getRegistryKey().equals(AetherDimensions.SHATTERED_SKY);
        ServerWorld target = player.getServer().getWorld(
                inSky ? World.OVERWORLD : AetherDimensions.SHATTERED_SKY);
        if (target == null) {
            player.sendMessage(Text.translatable("aetherius.rift.unavailable").formatted(Formatting.RED), true);
            return;
        }

        double x = player.getX();
        double z = player.getZ();
        double y;
        if (inSky) {
            BlockPos spawn = target.getSpawnPos();
            x = spawn.getX() + 0.5;
            z = spawn.getZ() + 0.5;
            y = target.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, spawn.getX(), spawn.getZ()) + 1.0;
        } else {
            int surface = ShatteredSkyChunkGenerator.surfaceHeight((int) Math.floor(x), (int) Math.floor(z),
                    target.getSeed());
            y = Math.max(ShatteredSkyChunkGenerator.MIN_Y + 2, surface + 2.0);
            if (surface <= ShatteredSkyChunkGenerator.MIN_Y) {
                // Aim for the nearest island centre if this column is empty sky.
                BlockPos island = ShatteredSkyChunkGenerator.nearestIslandCentre(x, z, target.getSeed());
                x = island.getX() + 0.5;
                z = island.getZ() + 0.5;
                y = island.getY() + 3.0;
            }
            ChronicleFlags.markSkyReached(player);
        }

        player.teleport(target, x, y, z, player.getYaw(), player.getPitch());
        player.setPortalCooldown(300);
        player.fallDistance = 0f;
        target.playSound(null, player.getBlockPos(), ModSounds.SPELL_NOVA_WARP, SoundCategory.PLAYERS, 1.4f, 0.9f);
        target.spawnParticles(com.aetherius.registry.ModParticles.STAR_DUST,
                x, y + 1, z, 80, 1.2, 1.2, 1.2, 0.2);
    }

    /** Tiny helper so this class does not need a direct Chronicle dependency in its header. */
    private static final class ChronicleFlags {
        static void markSkyReached(ServerPlayerEntity player) {
            com.aetherius.api.chronicle.ChronicleManager.get().onFlag(player,
                    com.aetherius.api.chronicle.ChronicleCounters.FLAG_SHATTERED_SKY_REACHED);
        }
    }

    public static RegistryKey<World> skyKey() {
        return AetherDimensions.SHATTERED_SKY;
    }
}
