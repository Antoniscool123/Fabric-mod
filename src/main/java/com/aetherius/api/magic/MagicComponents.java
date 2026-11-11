package com.aetherius.api.magic;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Static access point for the player magic capability.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * PlayerMagicCapability magic = MagicComponents.get(player);
 * magic.addMana(20f);
 * }</pre>
 */
public final class MagicComponents {
    private MagicComponents() {
    }

    public static PlayerMagicCapability get(PlayerEntity player) {
        return ((MagicHolder) player).aetherius$getMagic();
    }

    public static PlayerMagicCapability getNullable(@Nullable PlayerEntity player) {
        return player == null ? null : get(player);
    }

    /** @return true when the player is a server player (authoritative simulation). */
    public static boolean isAuthoritative(PlayerEntity player) {
        return !player.getWorld().isClient();
    }
}
