package com.aetherius.registry;

import com.aetherius.util.Aeth;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Status effects. These are gameplay-visible debuffs/buffs that complement the two
 * resource meters; the meters themselves live on the capability, not as effects, so they
 * can be drawn by the custom HUD instead of the vanilla potion list.
 */
public final class ModStatusEffects {

    /** Astral Backfire debuff: total darkness with faint rune after-images. */
    public static final StatusEffect CELESTIAL_BLINDNESS =
            register("celestial_blindness", StatusEffectCategory.HARMFUL, 0x9B4DFF);

    /** Applied while Mana Volatility is active - purely informational for HUD/particles. */
    public static final StatusEffect MANA_VOLATILITY =
            register("mana_volatility", StatusEffectCategory.NEUTRAL, 0xFF8A3D);

    /** Nox'Thal phase 2: nausea-like disorientation without the green vignette. */
    public static final StatusEffect GRAVITY_SICKNESS =
            register("gravity_sickness", StatusEffectCategory.HARMFUL, 0xD9E7FF);

    /** Purification rite blessing: blight cannot spread through the player's chunk. */
    public static final StatusEffect STARCLEANSE =
            register("starcleanse", StatusEffectCategory.BENEFICIAL, 0x8FD6FF);

    private ModStatusEffects() {
    }

    private static StatusEffect register(String path, StatusEffectCategory category, int color) {
        return Registry.register(Registries.STATUS_EFFECT, Aeth.id(path),
                new AetherStatusEffect(category, color));
    }

    /**
     * StatusEffect's constructor is protected, so every effect is created through this
     * subclass rather than by instantiating StatusEffect directly.
     */
    private static final class AetherStatusEffect extends StatusEffect {
        private AetherStatusEffect(StatusEffectCategory category, int color) {
            super(category, color);
        }
    }

    public static void register() {
    }
}
