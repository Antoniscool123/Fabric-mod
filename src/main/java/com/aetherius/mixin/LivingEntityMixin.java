package com.aetherius.mixin;

import com.aetherius.api.magic.MagicComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inverted-gravity quality of life.
 *
 * <p>When gravity is flipped, vanilla jump and fall damage both point the wrong way: jumping
 * would launch you into the ceiling and "falling" upward would kill you on landing. This mixin
 * reverses the jump impulse and suppresses fall damage while the inversion is active, so the
 * mechanic is disorienting rather than instantly lethal.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void aetherius$invertedJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player && MagicComponents.get(player).isGravityInverted()) {
            var velocity = player.getVelocity();
            player.setVelocity(velocity.x, -0.42, velocity.z);
            player.velocityModified = true;
            ci.cancel();
        }
    }

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void aetherius$invertedFallDamage(float fallDistance, float damageMultiplier, DamageSource source,
                                              CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player && MagicComponents.get(player).isGravityInverted()) {
            player.fallDistance = 0f;
            cir.setReturnValue(false);
        }
    }
}
