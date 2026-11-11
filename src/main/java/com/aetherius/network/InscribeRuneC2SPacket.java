package com.aetherius.network;

import com.aetherius.api.magic.Rune;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

/**
 * Client request to inscribe (append) a rune onto the held wand, or to clear it.
 * Action 0 = clear, action 1 = append.
 */
public final class InscribeRuneC2SPacket {

    public static final int ACTION_CLEAR = 0;
    public static final int ACTION_APPEND = 1;

    private InscribeRuneC2SPacket() {
    }

    public static void clear() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(ACTION_CLEAR);
        buf.writeByte(-1);
        ClientPlayNetworking.send(AetherNetworking.INSCRIBE_RUNE, buf);
    }

    public static void append(Rune rune) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(ACTION_APPEND);
        buf.writeByte(rune.ordinal());
        ClientPlayNetworking.send(AetherNetworking.INSCRIBE_RUNE, buf);
    }
}
