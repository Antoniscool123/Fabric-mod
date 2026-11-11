package com.aetherius.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Result of a cast attempt - drives the client's action-bar text, sound and HUD flash. */
public final class CastFeedbackS2CPacket {

    private CastFeedbackS2CPacket() {
    }

    public static void send(ServerPlayerEntity player, boolean success, @Nullable Identifier spellId,
                            String messageKey) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(success);
        buf.writeBoolean(spellId != null);
        if (spellId != null) {
            buf.writeIdentifier(spellId);
        }
        buf.writeString(messageKey, 128);
        ServerPlayNetworking.send(player, AetherNetworking.CAST_FEEDBACK, buf);
    }

    public static Feedback read(PacketByteBuf buf) {
        boolean success = buf.readBoolean();
        Identifier id = buf.readBoolean() ? buf.readIdentifier() : null;
        return new Feedback(success, id, buf.readString(128));
    }

    public record Feedback(boolean success, @Nullable Identifier spellId, String messageKey) {
    }
}
