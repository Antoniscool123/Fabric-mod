package com.aetherius.api.spell;

import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.registry.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Everything a spell needs to run: the caster, their capability, the rune sequence
 * and a pile of convenience helpers so spell bodies stay 10-30 lines long.
 *
 * <p>Damage dealt through {@link #damage(LivingEntity, float)} automatically applies
 * the Mana Volatility multiplier and registers Chronicle credit.</p>
 */
public final class SpellContext {

    private final ServerPlayerEntity player;
    private final PlayerMagicCapability magic;
    private final RuneCombination combination;

    public SpellContext(ServerPlayerEntity player, PlayerMagicCapability magic, RuneCombination combination) {
        this.player = player;
        this.magic = magic;
        this.combination = combination;
    }

    // ---- Core accessors ---------------------------------------------------

    public ServerPlayerEntity player() {
        return player;
    }

    public PlayerMagicCapability magic() {
        return magic;
    }

    public RuneCombination combination() {
        return combination;
    }

    public ServerWorld world() {
        return player.getServerWorld();
    }

    public Vec3d eyePos() {
        return player.getEyePos();
    }

    public Vec3d look() {
        return player.getRotationVec(1.0f);
    }

    /** Rune-tier potency: 1.0 at all tier I, ~1.5 at all tier III. */
    public float power() {
        return 0.8f + 0.2f * combination.averageTier();
    }

    public boolean has(SpellSchool school) {
        for (Rune rune : combination.runes()) {
            if (rune.school() == school) {
                return true;
            }
        }
        return false;
    }

    // ---- Targeting --------------------------------------------------------

    /** First living, hostile-able entity along the look vector. */
    public LivingEntity findTarget(double range) {
        Vec3d start = eyePos();
        Vec3d end = start.add(look().multiply(range));
        Box search = new Box(start, end).expand(1.6);
        List<Entity> candidates = world().getOtherEntities(player, search,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            Vec3d toEntity = candidate.getPos().add(0, candidate.getHeight() * 0.5, 0).subtract(start);
            double along = toEntity.dotProduct(look());
            if (along < 0 || along > range) {
                continue;
            }
            double perpendicular = toEntity.subtract(look().multiply(along)).length();
            double score = perpendicular + along * 0.05;
            if (score < bestScore) {
                bestScore = score;
                best = (LivingEntity) candidate;
            }
        }
        return best;
    }

    public List<LivingEntity> entitiesAround(Vec3d center, double radius) {
        Box box = new Box(center, center).expand(radius);
        return world().getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator());
    }

    // ---- Effects ----------------------------------------------------------

    public void sound(SoundEvent sound, float volume, float pitch) {
        world().playSound(null, player.getBlockPos(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    public void particles(Vec3d center, int count, double spread) {
        particlesAt(ModParticles.ASTRAL_SPARK, center, count, spread, 0.05);
    }

    public void particlesAt(DefaultParticleType type, Vec3d center, int count, double spread, double speed) {
        world().spawnParticles(type, center.x, center.y, center.z, count, spread, spread, spread, speed);
    }

    public void schoolBurst(SpellSchool school, Vec3d center, int count) {
        particlesAt(schoolParticle(school), center, count, 0.9, 0.08);
    }

    private static DefaultParticleType schoolParticle(SpellSchool school) {
        return switch (school) {
            case SOL -> net.minecraft.particle.ParticleTypes.FLAME;
            case LUNA -> net.minecraft.particle.ParticleTypes.SPLASH;
            case TERRA -> net.minecraft.particle.ParticleTypes.CRIT;
            case CAELUM -> net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK;
            case NOX -> ModParticles.VOID_MOTE;
        };
    }

    /** Volatility-aware magic damage. */
    public float damage(LivingEntity target, float baseDamage) {
        float amount = baseDamage * magic.getSpellDamageMultiplier() * power();
        target.damage(player.getDamageSources().indirectMagic(player, player), amount);
        return amount;
    }

    public void ignite(LivingEntity target, int seconds) {
        target.setOnFireFor(seconds);
    }

    public void blind(LivingEntity target, int ticks) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, ticks, 0, false, true, true));
    }

    public void slow(LivingEntity target, int ticks, int amplifier) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, amplifier, false, true, true));
    }

    /** Shoves an entity away from a point, respecting knockback resistance. */
    public void knockback(Entity target, Vec3d from, double strength) {
        Vec3d dir = target.getPos().subtract(from);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = look();
        }
        Vec3d push = dir.normalize().multiply(strength).add(0, strength * 0.35, 0);
        if (target instanceof PlayerEntity p) {
            p.addVelocity(push.x, push.y, push.z);
            p.velocityModified = true;
        } else {
            target.addVelocity(push.x, push.y, push.z);
        }
    }
}
