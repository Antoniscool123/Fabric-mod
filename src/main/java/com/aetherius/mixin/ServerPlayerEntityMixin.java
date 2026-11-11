package com.aetherius.mixin;

import com.aetherius.api.magic.MagicComponents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copies the capability across respawn / dimension change.
 *
 * <p>{@code ServerPlayerEntity#copyFrom} is the exact moment the new player object exists and
 * the old one is still readable, so it is the correct hook for capability migration (the same
 * place Forge fires {@code Clone}). Progress, runes, mastery and Chronicle state all survive
 * death; mana and tension are partially reset inside
 * {@link com.aetherius.api.magic.PlayerMagicCapability#copyFrom(PlayerMagicCapability)}.</p>
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void aetherius$copyMagic(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        MagicComponents.get(self).copyFrom(MagicComponents.get(oldPlayer));
    }
}
