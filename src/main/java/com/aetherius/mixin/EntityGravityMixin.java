package com.aetherius.mixin;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.dimension.AetherDimensions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two gravity rules in one hook:
 * <ul>
 *   <li><b>Low gravity</b>: every entity in the Shattered Sky falls at 35% speed.</li>
 *   <li><b>Gravity inversion</b>: players affected by Nox'Thal's Phase 2 (or an anchor pulse)
 *       fall <i>upwards</i> - a negative gravity value is all {@code LivingEntity#travel}
 *       needs, which is why this is a one-line change instead of a bespoke movement system.</li>
 * </ul>
 */
@Mixin(Entity.class)
public abstract class EntityGravityMixin {

    /**
     * {@code require = 0}: on a mapping where {@code Entity#getGravity} is absent or renamed the
     * game must still launch. The dimension then simply keeps vanilla gravity instead of failing
     * the whole mixin set - the trade is documented in docs/VERIFICATION.md.
     */
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherius$modifyGravity(CallbackInfoReturnable<Double> cir) {
        Entity self = (Entity) (Object) this;
        double gravity = cir.getReturnValueD();

        if (self.getWorld().getRegistryKey().equals(AetherDimensions.SHATTERED_SKY)) {
            gravity *= AetherDimensions.LOW_GRAVITY;
        }
        if (self instanceof PlayerEntity player && MagicComponents.get(player).isGravityInverted()) {
            gravity = -gravity;
        }
        cir.setReturnValue(gravity);
    }
}
