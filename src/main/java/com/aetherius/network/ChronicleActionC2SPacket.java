package com.aetherius.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

/** Chronicle UI actions: open, refresh, track a quest. */
public final class ChronicleActionC2SPacket {

    public static final int ACTION_OPEN = 0;
    public static final int ACTION_REFRESH = 1;
    public static final int ACTION_TRACK = 2;

    private ChronicleActionC2SPacket() {
    }

    public static void send(int action, String questId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(action);
        buf.writeString(questId == null ? "" : questId, 128);
        ClientPlayNetworking.send(AetherNetworking.CHRONICLE_ACTION, buf);
    }
}
