package com.aetherius.network;

import com.aetherius.api.magic.Rune;
import com.aetherius.api.spell.RuneCombination;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

/**
 * Client request to cast the currently inscribed runes.
 *
 * <p>The client only says "cast this sequence from this hand". It never sends damage,
 * costs or targets - the server re-derives all of that.</p>
 */
public final class CastSpellC2SPacket {

    private CastSpellC2SPacket() {
    }

    public static void send(RuneCombination combination, Hand hand) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(hand == Hand.OFF_HAND ? 1 : 0);
        buf.writeByte(combination.size());
        for (Rune rune : combination.runes()) {
            buf.writeByte(rune.ordinal());
        }
        ClientPlayNetworking.send(AetherNetworking.CAST_SPELL, buf);
    }
}
