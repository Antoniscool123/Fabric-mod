package com.aetherius.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Triggers the client HUD shake. Sent to every player within the radius so the
 * Astral Backfire is felt by onlookers, not just the victim.
 */
public final class ScreenShakeS2CPacket {

    private ScreenShakeS2CPacket() {
    }

    public static void send(ServerWorld world, BlockPos origin, double radius, int intensity) {
        for (ServerPlayerEntity player : PlayerLookup.around(world, origin, radius)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(intensity);
            buf.writeVarInt(18); // duration in ticks
            ServerPlayNetworking.send(player, AetherNetworking.SCREEN_SHAKE, buf);
        }
    }

    public static Shake read(PacketByteBuf buf) {
        return new Shake(buf.readVarInt(), buf.readVarInt());
    }

    public record Shake(int intensity, int durationTicks) {
    }
}
