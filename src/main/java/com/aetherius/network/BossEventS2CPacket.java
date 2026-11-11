package com.aetherius.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Boss choreography channel: phase transitions, screen darkening, gravity pulses and
 * cutscene-ish beats that the client must render (custom boss bar colours, vignette,
 * eclipse overlay).
 */
public final class BossEventS2CPacket {

    public enum Kind {
        PHASE_CHANGE,
        SUPERNOVA_DARKEN,
        SUPERNOVA_END,
        GRAVITY_PULSE,
        ELYTHERA_LOCK,
        BOSS_DEATH
    }

    private BossEventS2CPacket() {
    }

    public static void send(ServerWorld world, Vec3d center, double radius, int entityId, Kind kind,
                            int phase, int durationTicks) {
        for (ServerPlayerEntity player : PlayerLookup.around(world, center, radius)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(entityId);
            buf.writeEnumConstant(kind);
            buf.writeVarInt(phase);
            buf.writeVarInt(durationTicks);
            ServerPlayNetworking.send(player, AetherNetworking.BOSS_EVENT, buf);
        }
    }

    public static BossEvent read(PacketByteBuf buf) {
        return new BossEvent(buf.readVarInt(), buf.readEnumConstant(Kind.class), buf.readVarInt(),
                buf.readVarInt());
    }

    public record BossEvent(int entityId, Kind kind, int phase, int durationTicks) {
    }
}
