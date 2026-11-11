package com.aetherius.network;

import com.aetherius.api.magic.PlayerMagicCapability;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Full Chronicle state (completed quests, flags, counters, lore unlocks, act).
 *
 * <p>Sent when the Chronicle screen is opened and after every quest completion, which is
 * why it can afford to be a plain NBT blob: it is not on the per-second path.</p>
 */
public final class ChronicleSyncS2CPacket {

    private ChronicleSyncS2CPacket() {
    }

    public static void send(ServerPlayerEntity player, PlayerMagicCapability magic) {
        NbtCompound nbt = new NbtCompound();
        magic.getChronicle().writeNbt(nbt);
        net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(nbt);
        ServerPlayNetworking.send(player, AetherNetworking.CHRONICLE_SYNC, buf);
    }

    public static NbtCompound read(net.minecraft.network.PacketByteBuf buf) {
        NbtCompound nbt = buf.readNbt();
        return nbt == null ? new NbtCompound() : nbt;
    }
}
