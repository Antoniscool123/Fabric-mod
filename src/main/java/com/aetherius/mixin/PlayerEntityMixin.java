package com.aetherius.mixin;

import com.aetherius.api.magic.MagicHolder;
import com.aetherius.api.magic.PlayerMagicCapability;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Attaches the magic capability to every player and persists it.
 *
 * <p>Three responsibilities:</p>
 * <ol>
 *   <li>Lazily create one {@link PlayerMagicCapability} per player instance.</li>
 *   <li>Serialise it into the player's custom NBT ({@code AetheriusMagic} tag) so it survives
 *       server restarts and single-player reloads.</li>
 *   <li>Hard-block elytra flight while a boss has the lock active - the per-tick flag lives
 *       on the capability, so both client and server agree without an extra packet.</li>
 * </ol>
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements MagicHolder {

    @Unique
    private PlayerMagicCapability aetherius$magic;

    @Override
    public PlayerMagicCapability aetherius$getMagic() {
        if (this.aetherius$magic == null) {
            this.aetherius$magic = new PlayerMagicCapability((PlayerEntity) (Object) this);
        }
        return this.aetherius$magic;
    }

    @Override
    public void aetherius$setMagic(PlayerMagicCapability capability) {
        this.aetherius$magic = capability;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void aetherius$writeMagic(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("AetheriusMagic", aetherius$getMagic().toNbt());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void aetherius$readMagic(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("AetheriusMagic")) {
            aetherius$getMagic().fromNbt(nbt.getCompound("AetheriusMagic"));
        }
    }

    /**
     * Elytra suppression. {@code checkFallFlying} is the single funnel vanilla uses before
     * letting a player start gliding, so cancelling it here also stops mid-air re-activation.
     */
    @Inject(method = "checkFallFlying", at = @At("HEAD"), cancellable = true)
    private void aetherius$lockElytra(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (aetherius$getMagic().isElytraLocked()) {
            if (self.isFallFlying()) {
                self.stopFallFlying();
            }
            cir.setReturnValue(false);
        }
    }
}
