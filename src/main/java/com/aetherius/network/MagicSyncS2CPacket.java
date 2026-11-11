package com.aetherius.network;

import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.api.magic.SpellSchool;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Authoritative magic state -> client mirror.
 *
 * <p>Sent on login, dimension change, quest completion and whenever the server-side
 * capability is flagged dirty (throttled to once per second by the Chronicle tick).
 * Payload is 24 bytes plus mastery bytes, so sending it every second is negligible.</p>
 */
public final class MagicSyncS2CPacket {

    private MagicSyncS2CPacket() {
    }

    public static void send(ServerPlayerEntity player, PlayerMagicCapability magic) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(magic.getMana());
        buf.writeFloat(magic.getMaxMana());
        buf.writeFloat(magic.getAstralTension());
        buf.writeFloat(magic.getTensionResistance());
        buf.writeShort(RuneBitmask.toMask(magic.getUnlockedRunes()));
        for (SpellSchool school : SpellSchool.values()) {
            buf.writeByte(magic.getSchoolMastery(school));
        }
        buf.writeVarInt(magic.getBackfireTicks());
        buf.writeVarInt(magic.getGravityInversionTicks());
        buf.writeVarInt(magic.getElytraLockTicks());
        buf.writeVarInt(magic.getCelestialBlindnessTicks());
        ServerPlayNetworking.send(player, AetherNetworking.MAGIC_SYNC, buf);
    }

    public static MagicSnapshot read(PacketByteBuf buf) {
        float mana = buf.readFloat();
        float maxMana = buf.readFloat();
        float tension = buf.readFloat();
        float tensionResistance = buf.readFloat();
        short runeMask = buf.readShort();
        byte[] mastery = new byte[SpellSchool.values().length];
        for (int i = 0; i < mastery.length; i++) {
            mastery[i] = buf.readByte();
        }
        return new MagicSnapshot(mana, maxMana, tension, tensionResistance, runeMask, mastery,
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    /** Immutable client-side snapshot; also handy for the HUD's animated bars. */
    public record MagicSnapshot(float mana, float maxMana, float tension, float tensionResistance,
                                short runeMask, byte[] mastery, int backfireTicks,
                                int gravityInversionTicks, int elytraLockTicks, int blindnessTicks) {

        public boolean volatileMana() {
            return tension >= PlayerMagicCapability.VOLATILITY_THRESHOLD;
        }

        public float manaRatio() {
            return maxMana <= 0 ? 0f : Math.min(1f, mana / maxMana);
        }

        public float tensionRatio() {
            return Math.min(1f, tension / 100f);
        }
    }
}
