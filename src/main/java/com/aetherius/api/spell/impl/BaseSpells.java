package com.aetherius.api.spell.impl;

import com.aetherius.api.magic.SpellSchool;
import com.aetherius.api.spell.AbstractSpell;
import com.aetherius.api.spell.SchoolPattern;
import com.aetherius.api.spell.SpellContext;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * The five tier-I school spells. One rune, always available, cheap enough to spam
 * while learning the system - and each one teaches a different verb
 * (strike / mend / defend / move / drain).
 */
public final class BaseSpells {

    private BaseSpells() {
    }

    public static void registerAll() {
        SpellRegistry.register(new EmberLance());
        SpellRegistry.register(new TideOfRenewal());
        SpellRegistry.register(new StoneBulwark());
        SpellRegistry.register(new GaleStep());
        SpellRegistry.register(new VoidRend());
    }

    /** SOL + - high damage, armor shredding, light. */
    public static final class EmberLance extends AbstractSpell {
        public EmberLance() {
            super("ember_lance", SpellSchool.SOL, SchoolPattern.of(SpellSchool.SOL), 20);
        }

        @Override
        public void cast(SpellContext ctx) {
            LivingEntity target = ctx.findTarget(18.0);
            Vec3d impact = target != null ? target.getPos().add(0, target.getHeight() * 0.5, 0)
                    : ctx.eyePos().add(ctx.look().multiply(18.0));
            if (target != null) {
                ctx.damage(target, 7.0f);
                ctx.ignite(target, 6);
                // Armor shred: weakness cuts the target's melee output while the burn ticks.
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
                ctx.particlesAt(ModParticles.SOL_EMBER, impact, 30, 0.4, 0.08);
            }
            ctx.schoolBurst(SpellSchool.SOL, impact, 24);
            ctx.sound(ModSounds.SPELL_SOL, 1.0f, 1.1f);
        }
    }

    /** LUNA - regeneration, freezing, fluid manipulation. */
    public static final class TideOfRenewal extends AbstractSpell {
        public TideOfRenewal() {
            super("tide_of_renewal", SpellSchool.LUNA, SchoolPattern.of(SpellSchool.LUNA), 60);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 1));
            player.extinguish();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0));
            for (LivingEntity other : ctx.entitiesAround(player.getPos(), 5.0)) {
                ctx.slow(other, 100, 2);
                ctx.damage(other, 2.0f);
            }
            ctx.particles(player.getPos().add(0, 1, 0), 60, 1.2);
            ctx.sound(ModSounds.SPELL_LUNA, 1.0f, 0.9f);
        }
    }

    /** TERRA - defense, wall generation, ore transmutation. */
    public static final class StoneBulwark extends AbstractSpell {
        public StoneBulwark() {
            super("stone_bulwark", SpellSchool.TERRA, SchoolPattern.of(SpellSchool.TERRA), 100);
        }

        @Override
        public void cast(SpellContext ctx) {
            BlockPos origin = ctx.player().getBlockPos();
            Direction facing = ctx.player().getHorizontalFacing();
            for (int width = -1; width <= 1; width++) {
                for (int height = 0; height <= 2; height++) {
                    BlockPos pos = origin.offset(facing, 2)
                            .offset(facing.rotateYClockwise(), width)
                            .up(height);
                    if (ctx.world().getBlockState(pos).isAir()) {
                        ctx.world().setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
                    }
                }
            }
            ctx.sound(ModSounds.SPELL_TERRA, 1.0f, 0.7f);
            ctx.particles(ctx.player().getPos().add(0, 1, 0), 30, 1.0);
        }
    }

    /** CAELUM - flight, knockback, lightning. */
    public static final class GaleStep extends AbstractSpell {
        public GaleStep() {
            super("gale_step", SpellSchool.CAELUM, SchoolPattern.of(SpellSchool.CAELUM), 40);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            Vec3d impulse = ctx.look().multiply(1.35).add(0, 0.42, 0);
            player.addVelocity(impulse.x, impulse.y, impulse.z);
            player.velocityModified = true;
            player.fallDistance = 0f;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
            for (LivingEntity other : ctx.entitiesAround(player.getPos(), 3.5)) {
                ctx.knockback(other, player.getPos(), 0.8);
            }
            ctx.sound(ModSounds.SPELL_CAELUM, 1.0f, 1.4f);
            ctx.particlesAt(ModParticles.ASTRAL_SPARK, player.getPos().add(0, 0.5, 0), 28, 0.6, 0.1);
        }
    }

    /** NOX - gravity, teleportation, life-drain. The only base spell that feeds Tension. */
    public static final class VoidRend extends AbstractSpell {
        public VoidRend() {
            super("void_rend", SpellSchool.NOX, SchoolPattern.of(SpellSchool.NOX), 30);
        }

        @Override
        public void cast(SpellContext ctx) {
            LivingEntity target = ctx.findTarget(12.0);
            if (target != null) {
                float dealt = ctx.damage(target, 5.0f);
                // Life-drain: heals a fraction of damage dealt, never above the mana cap.
                ctx.player().heal(Math.min(4.0f, dealt * 0.5f));
                ctx.slow(target, 60, 1);
                ctx.particlesAt(ModParticles.VOID_MOTE, target.getPos().add(0, 1, 0), 40, 0.5, 0.12);
            }
            ctx.sound(ModSounds.SPELL_NOX, 1.0f, 0.6f);
        }
    }
}
