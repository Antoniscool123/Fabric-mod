package com.aetherius.api.magic;

/**
 * Duck-typing interface injected into {@link net.minecraft.entity.player.PlayerEntity}
 * by {@code PlayerEntityMixin}. This is the Fabric equivalent of Forge's
 * {@code Capability<PlayerMagicCapability>} provider: one attachment per player,
 * lazily created, serialised with the player and copied on respawn.
 *
 * <p>Never cast a {@code PlayerEntity} to this interface directly - go through
 * {@link MagicComponents#get(net.minecraft.entity.player.PlayerEntity)}.</p>
 */
public interface MagicHolder {

    /** @return the live, never-null capability instance for this player. */
    PlayerMagicCapability aetherius$getMagic();

    /** Restores a capability instance after a player clone (dimension change / respawn). */
    void aetherius$setMagic(PlayerMagicCapability capability);
}
