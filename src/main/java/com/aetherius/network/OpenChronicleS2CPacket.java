package com.aetherius.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** Empty payload: "open the Chronicle book now". Sent by the Chronicle Tome item. */
public final class OpenChronicleS2CPacket {

    private OpenChronicleS2CPacket() {
    }

    public static void send(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, AetherNetworking.OPEN_CHRONICLE, PacketByteBufs.empty());
    }
}
