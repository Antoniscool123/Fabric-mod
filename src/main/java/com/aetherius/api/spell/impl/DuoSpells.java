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
import net.minecraft.util.math.Vec3d;

/**
 * Two-rune combinations. These are the workhorses of mid-game (Act II) and each
 * pairs two schools in a way that fixes the weakness of the other:
 * Sol needs range, Luna needs burst, Terra needs tempo, Caelum needs staying power,
 * Nox needs survivability.
 */
public final class DuoSpells {

    private DuoSpells() {
    }

    public static void registerAll() {
        SpellRegistry.register(new SolarFlare());
        SpellRegistry.register(new Stormfire());
        SpellRegistry.register(new FrostLance());
        SpellRegistry.register(new Rampart());
        SpellRegistry.register(new GravityWell());
        SpellRegistry.register(new MireOfStars());
        SpellRegistry.register(new UmbralStep());
    }

    /** SOL + SOL - close-range fire nova. */
    public static final class SolarFlare extends AbstractSpell {
        public SolarFlare() {
            super("solar_flare", SpellSchool.SOL, SchoolPattern.of(SpellSchool.SOL, SpellSchool.SOL), 80);
        }

        @Override
        public void cast(SpellContext ctx) {
            Vec3d center = ctx.eyePos().add(ctx.look().multiply(6.0));
            for (LivingEntity target : ctx.entitiesAround(center, 4.5)) {
                ctx.damage(target, 10.0f);
                ctx.ignite(target, 8);
                ctx.knockback(target, center, 0.6);
            }
            ctx.schoolBurst(SpellSchool.SOL, center, 90);
            ctx.particlesAt(ModParticles.SOL_EMBER, center, 60, 2.2, 0.14);
            ctx.sound(ModSounds.SPELL_SOL, 1.4f, 0.8f);
        }
    }

    /** SOL + CAELUM - lightning strike that ignites the impact ground. */
    public static final class Stormfire extends AbstractSpell {
        public Stormfire() {
            super("stormfire", SpellSchool.CAELUM, SchoolPattern.of(SpellSchool.SOL, SpellSchool.CAELUM), 100);
        }

        @Override
        public void cast(SpellContext ctx) {
            LivingEntity target = ctx.findTarget(26.0);
            BlockPos impact = target != null ? target.getBlockPos()
                    : BlockPos.ofFloored(ctx.eyePos().add(ctx.look().multiply(26.0)));
            net.minecraft.entity.LightningEntity bolt =
                    net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(ctx.world());
            if (bolt != null) {
                bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(impact));
                if (target != null) {
                    bolt.setChanneler(ctx.player());
                }
                ctx.world().spawnEntity(bolt);
            }
            if (target != null) {
                ctx.ignite(target, 10);
                ctx.damage(target, 6.0f);
            }
            ctx.sound(ModSounds.SPELL_CAELUM, 1.5f, 0.7f);
        }
    }

    /** LUNA + CAELUM - piercing frost that freezes movement. */
    public static final class FrostLance extends AbstractSpell {
        public FrostLance() {
            super("frost_lance", SpellSchool.LUNA, SchoolPattern.of(SpellSchool.LUNA, SpellSchool.CAELUM), 70);
        }

        @Override
        public void cast(SpellContext ctx) {
            LivingEntity target = ctx.findTarget(22.0);
            if (target == null) {
                return;
            }
            ctx.damage(target, 8.0f);
            ctx.slow(target, 160, 3);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 120, 1));
            target.setFrozenTicks(200);
            ctx.particlesAt(net.minecraft.particle.ParticleTypes.SNOWFLAKE,
                    target.getPos().add(0, 1, 0), 50, 0.6, 0.1);
            ctx.sound(ModSounds.SPELL_LUNA, 1.2f, 1.3f);
        }
    }

    /** TERRA + TERRA - a 5-wide, 4-high siege wall. */
    public static final class Rampart extends AbstractSpell {
        public Rampart() {
            super("rampart", SpellSchool.TERRA, SchoolPattern.of(SpellSchool.TERRA, SpellSchool.TERRA), 160);
        }

        @Override
        public void cast(SpellContext ctx) {
            BlockPos origin = ctx.player().getBlockPos();
            for (int width = -2; width <= 2; width++) {
                for (int height = 0; height <= 3; height++) {
                    for (int depth = 2; depth <= 3; depth++) {
                        BlockPos pos = origin.offset(ctx.player().getHorizontalFacing(), depth)
                                .offset(ctx.player().getHorizontalFacing().rotateYClockwise(), width)
                                .up(height);
                        if (ctx.world().getBlockState(pos).isAir()) {
                            ctx.world().setBlockState(pos, Blocks.DEEPSLATE_BRICKS.getDefaultState());
                        }
                    }
                }
            }
            ctx.sound(ModSounds.SPELL_TERRA, 1.2f, 0.6f);
        }
    }

    /** NOX + NOX - a singularity that drags mobs toward a point. */
    public static final class GravityWell extends AbstractSpell {
        public GravityWell() {
            super("gravity_well", SpellSchool.NOX, SchoolPattern.of(SpellSchool.NOX, SpellSchool.NOX), 140);
        }

        @Override
        public void cast(SpellContext ctx) {
            Vec3d center = ctx.eyePos().add(ctx.look().multiply(8.0));
            for (LivingEntity target : ctx.entitiesAround(center, 7.0)) {
                Vec3d pull = center.subtract(target.getPos().add(0, 1, 0)).normalize().multiply(0.9);
                target.addVelocity(pull.x, pull.y * 0.5, pull.z);
                if (target instanceof ServerPlayerEntity sp) {
                    sp.velocityModified = true;
                }
                ctx.slow(target, 60, 2);
                ctx.damage(target, 2.5f);
            }
            ctx.particlesAt(ModParticles.VOID_MOTE, center, 120, 3.0, -0.15);
            ctx.sound(ModSounds.SPELL_NOX, 1.4f, 0.5f);
        }
    }

    /** TERRA + LUNA - grasping bog that heals the caster and roots enemies. */
    public static final class MireOfStars extends AbstractSpell {
        public MireOfStars() {
            super("mire_of_stars", SpellSchool.TERRA, SchoolPattern.of(SpellSchool.TERRA, SpellSchool.LUNA), 120);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            for (LivingEntity target : ctx.entitiesAround(player.getPos(), 6.0)) {
                ctx.slow(target, 140, 4);
                ctx.damage(target, 3.0f);
            }
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
            player.heal(4.0f);
            ctx.particlesAt(net.minecraft.particle.ParticleTypes.FALLING_DRIPSTONE_WATER,
                    player.getPos().add(0, 0.2, 0), 80, 3.0, 0.02);
            ctx.sound(ModSounds.SPELL_LUNA, 1.0f, 0.7f);
        }
    }

    /** CAELUM + NOX - short-range blink that leaves a concussive wake. */
    public static final class UmbralStep extends AbstractSpell {
        public UmbralStep() {
            super("umbral_step", SpellSchool.NOX, SchoolPattern.of(SpellSchool.CAELUM, SpellSchool.NOX), 90);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            Vec3d from = player.getPos();
            net.minecraft.util.hit.HitResult hit = player.raycast(14.0, 0f, false);
            BlockPos dest = hit instanceof net.minecraft.util.hit.BlockHitResult blockHit
                    ? blockHit.getBlockPos().offset(blockHit.getSide())
                    : player.getBlockPos();
            for (LivingEntity target : ctx.entitiesAround(from, 3.0)) {
                ctx.knockback(target, from, 0.9);
                ctx.damage(target, 3.0f);
            }
            player.teleport(player.getServerWorld(), dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                    player.getYaw(), player.getPitch());
            player.fallDistance = 0f;
            ctx.sound(ModSounds.SPELL_NOX, 1.0f, 1.2f);
            ctx.particles(from.add(0, 1, 0), 40, 1.0);
        }
    }
}
