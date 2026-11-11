package com.aetherius.api.spell.impl;

import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.api.spell.AbstractSpell;
import com.aetherius.api.spell.SchoolPattern;
import com.aetherius.api.spell.SpellContext;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Fallback spell for any rune sequence that has no authored combination.
 *
 * <p>Design intent: an un-authored sequence should never feel like a bug. Each school
 * present in the sequence contributes a small effect, scaled by rune tier, so
 * experimentation always gives readable feedback - at full mana and Tension cost.</p>
 */
public final class ResonantEchoSpell extends AbstractSpell {

    public static final ResonantEchoSpell INSTANCE = new ResonantEchoSpell();

    private ResonantEchoSpell() {
        super("resonant_echo", SpellSchool.CAELUM, SchoolPattern.of(SpellSchool.CAELUM), 120);
    }

    @Override
    public void cast(SpellContext ctx) {
        LivingEntity target = ctx.findTarget(14.0);
        for (Rune rune : ctx.combination().runes()) {
            float power = 2.0f + rune.tier();
            switch (rune.school()) {
                case SOL -> {
                    if (target != null) {
                        ctx.damage(target, power);
                        ctx.ignite(target, 3 + rune.tier());
                    }
                    ctx.particlesAt(ModParticles.SOL_EMBER, ctx.eyePos().add(ctx.look().multiply(6)), 18, 0.7, 0.06);
                }
                case LUNA -> {
                    ctx.player().heal(power * 0.5f);
                    if (target != null) {
                        ctx.slow(target, 60, 1);
                    }
                    ctx.schoolBurst(SpellSchool.LUNA, ctx.player().getPos().add(0, 1, 0), 18);
                }
                case TERRA -> {
                    ctx.player().addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 0));
                    ctx.schoolBurst(SpellSchool.TERRA, ctx.player().getPos().add(0, 1, 0), 18);
                }
                case CAELUM -> {
                    var impulse = ctx.look().multiply(0.35 + 0.1 * rune.tier()).add(0, 0.2, 0);
                    ctx.player().addVelocity(impulse.x, impulse.y, impulse.z);
                    ctx.player().velocityModified = true;
                    ctx.schoolBurst(SpellSchool.CAELUM, ctx.player().getPos().add(0, 1, 0), 18);
                }
                case NOX -> {
                    if (target != null) {
                        ctx.damage(target, power * 0.8f);
                        ctx.magic().addAstralTension(4f * rune.tier());
                    }
                    ctx.particlesAt(ModParticles.VOID_MOTE, ctx.player().getPos().add(0, 1, 0), 24, 1.0, 0.08);
                }
            }
        }
        ctx.sound(ModSounds.SPELL_RESONANT, 0.9f, 1.2f);
    }
}
