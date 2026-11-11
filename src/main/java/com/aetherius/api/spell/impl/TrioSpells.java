package com.aetherius.api.spell.impl;

import com.aetherius.api.magic.SpellSchool;
import com.aetherius.api.spell.AbstractSpell;
import com.aetherius.api.spell.SchoolPattern;
import com.aetherius.api.spell.SpellContext;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Three-rune combinations - the ceiling of the system. These cost 3 runes' worth of
 * mana, most of them push Astral Tension hard, and several are gated behind
 * Chronicle milestones (Act II unlock) via {@link #canCast}.
 *
 * <p>The headline cast is {@code NOVA WARP}: Caelum (movement) resolves first as the
 * blink, Sol (light) detonates on arrival, Nox (void) pays the toll in Tension.</p>
 */
public final class TrioSpells {

    private TrioSpells() {
    }

    public static void registerAll() {
        SpellRegistry.register(new NovaWarp());
        SpellRegistry.register(new StarfallJudgement());
        SpellRegistry.register(new AegisOfTides());
        SpellRegistry.register(new EclipseDevour());
        SpellRegistry.register(new SkybreakAscension());
        SpellRegistry.register(new ForgeheartTransmutation());
    }

    /**
     * CAELUM + SOL + NOX = <b>Nova Warp</b>.
     * Teleports the caster to the aimed location and releases a blind-inducing fire
     * burst where they land.
     */
    public static final class NovaWarp extends AbstractSpell {
        public NovaWarp() {
            super("nova_warp", SpellSchool.CAELUM,
                    SchoolPattern.of(SpellSchool.CAELUM, SpellSchool.SOL, SpellSchool.NOX), 200);
        }

        @Override
        public boolean canCast(SpellContext context) {
            return context.magic().getChronicle().hasMilestone("act2_attuned");
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            ServerWorld world = ctx.world();
            Vec3d origin = player.getPos();
            net.minecraft.util.hit.HitResult hit = player.raycast(28.0, 0f, false);
            BlockPos landing = hit instanceof net.minecraft.util.hit.BlockHitResult blockHit
                    ? blockHit.getBlockPos().offset(blockHit.getSide())
                    : player.getBlockPos();

            player.teleport(world, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5,
                    player.getYaw(), player.getPitch());
            player.fallDistance = 0f;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0));

            Vec3d detonation = Vec3d.ofBottomCenter(landing).add(0, 1, 0);
            for (LivingEntity target : ctx.entitiesAround(detonation, 5.5)) {
                ctx.damage(target, 12.0f);
                ctx.ignite(target, 10);
                ctx.blind(target, 140);
                ctx.knockback(target, detonation, 0.8);
            }
            ctx.particlesAt(ModParticles.SOL_EMBER, detonation, 140, 2.4, 0.2);
            ctx.particlesAt(ModParticles.VOID_MOTE, origin.add(0, 1, 0), 90, 1.2, 0.15);
            ctx.sound(ModSounds.SPELL_NOVA_WARP, 1.6f, 0.9f);
            // Nova Warp is non-destructive by design: the burst damages entities only.
            world.spawnParticles(net.minecraft.particle.ParticleTypes.EXPLOSION_EMITTER,
                    detonation.x, detonation.y, detonation.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** SOL + SOL + CAELUM = <b>Starfall Judgement</b>: a meteor barrage on a target area. */
    public static final class StarfallJudgement extends AbstractSpell {
        public StarfallJudgement() {
            super("starfall_judgement", SpellSchool.SOL,
                    SchoolPattern.of(SpellSchool.SOL, SpellSchool.SOL, SpellSchool.CAELUM), 260);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerWorld world = ctx.world();
            Vec3d center = ctx.eyePos().add(ctx.look().multiply(18.0));
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                double radius = 2.0 + world.random.nextDouble() * 4.0;
                BlockPos impact = BlockPos.ofFloored(center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
                FallingBlockEntity meteor = FallingBlockEntity.spawnFromBlock(world,
                        impact.add(0, 18, 0), Blocks.MAGMA_BLOCK.getDefaultState());
                if (meteor != null) {
                    meteor.setVelocity(0, -1.4, 0);
                    meteor.velocityModified = true;
                }
                world.createExplosion(ctx.player(), impact.getX() + 0.5, impact.getY() + 0.5, impact.getZ() + 0.5,
                        1.3f, World.ExplosionSourceType.BLOCK);
                ctx.particlesAt(ModParticles.SOL_EMBER, Vec3d.ofBottomCenter(impact), 40, 1.0, 0.2);
            }
            ctx.sound(ModSounds.SPELL_NOVA_WARP, 1.5f, 0.6f);
        }
    }

    /** LUNA + TERRA + TERRA = <b>Aegis of Tides</b>: a healing shell that punishes melee. */
    public static final class AegisOfTides extends AbstractSpell {
        public AegisOfTides() {
            super("aegis_of_tides", SpellSchool.TERRA,
                    SchoolPattern.of(SpellSchool.LUNA, SpellSchool.TERRA, SpellSchool.TERRA), 240);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 3));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 2));
            for (LivingEntity target : ctx.entitiesAround(player.getPos(), 5.0)) {
                ctx.knockback(target, player.getPos(), 1.2);
                ctx.slow(target, 80, 2);
            }
            ctx.particlesAt(net.minecraft.particle.ParticleTypes.SPLASH, player.getPos().add(0, 1, 0), 120, 2.0, 0.15);
            ctx.sound(ModSounds.SPELL_LUNA, 1.3f, 0.8f);
        }
    }

    /** NOX + NOX + NOX = <b>Eclipse Devour</b>: mass life-drain, the fastest way to a backfire. */
    public static final class EclipseDevour extends AbstractSpell {
        public EclipseDevour() {
            super("eclipse_devour", SpellSchool.NOX,
                    SchoolPattern.of(SpellSchool.NOX, SpellSchool.NOX, SpellSchool.NOX), 300);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            float drained = 0f;
            for (LivingEntity target : ctx.entitiesAround(player.getPos(), 11.0)) {
                drained += ctx.damage(target, 9.0f);
                ctx.blind(target, 120);
                ctx.slow(target, 120, 2);
            }
            player.heal(Math.min(14.0f, drained * 0.35f));
            ctx.particlesAt(ModParticles.VOID_MOTE, player.getPos().add(0, 1, 0), 200, 4.0, -0.1);
            ctx.sound(ModSounds.SPELL_NOX, 1.8f, 0.4f);
            // This is deliberately the single biggest Tension spike in the mod.
            ctx.magic().addAstralTension(18f);
        }
    }

    /** CAELUM + CAELUM + CAELUM = <b>Skybreak Ascension</b>: true flight, then a lightning crown. */
    public static final class SkybreakAscension extends AbstractSpell {
        public SkybreakAscension() {
            super("skybreak_ascension", SpellSchool.CAELUM,
                    SchoolPattern.of(SpellSchool.CAELUM, SpellSchool.CAELUM, SpellSchool.CAELUM), 280);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerPlayerEntity player = ctx.player();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 70, 2, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 300, 0));
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                BlockPos pos = player.getBlockPos().add(
                        (int) Math.round(Math.cos(angle) * 6), 0, (int) Math.round(Math.sin(angle) * 6));
                net.minecraft.entity.LightningEntity bolt =
                        net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(ctx.world());
                if (bolt != null) {
                    bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos));
                    ctx.world().spawnEntity(bolt);
                }
            }
            ctx.sound(ModSounds.SPELL_CAELUM, 1.8f, 0.9f);
        }
    }

    /** TERRA + TERRA + SOL = <b>Forgeheart Transmutation</b>: converts stone into ore veins. */
    public static final class ForgeheartTransmutation extends AbstractSpell {
        public ForgeheartTransmutation() {
            super("forgeheart_transmutation", SpellSchool.TERRA,
                    SchoolPattern.of(SpellSchool.TERRA, SpellSchool.TERRA, SpellSchool.SOL), 320);
        }

        @Override
        public void cast(SpellContext ctx) {
            ServerWorld world = ctx.world();
            BlockPos origin = ctx.player().getBlockPos();
            int converted = 0;
            for (BlockPos pos : BlockPos.iterateOutwards(origin, 7, 4, 7)) {
                if (converted >= 24) {
                    break;
                }
                var state = world.getBlockState(pos);
                if (!state.isOf(Blocks.STONE) && !state.isOf(Blocks.DEEPSLATE)) {
                    continue;
                }
                if (world.random.nextFloat() > 0.18f) {
                    continue;
                }
                int roll = world.random.nextInt(100);
                var ore = roll < 55 ? Blocks.IRON_ORE
                        : roll < 78 ? Blocks.COPPER_ORE
                        : roll < 92 ? Blocks.REDSTONE_ORE
                        : Blocks.GOLD_ORE;
                world.setBlockState(pos, ore.getDefaultState());
                converted++;
                ctx.particlesAt(ModParticles.SOL_EMBER, Vec3d.ofCenter(pos), 8, 0.4, 0.02);
            }
            ctx.sound(ModSounds.SPELL_TERRA, 1.2f, 1.1f);
        }
    }
}
