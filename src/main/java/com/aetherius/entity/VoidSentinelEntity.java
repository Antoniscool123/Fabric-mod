package com.aetherius.entity;

import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Void Sentinel - the Act I elite.
 *
 * <h2>Shield</h2>
 * Raises a celestial bulwark that absorbs <b>80%</b> of incoming projectile damage while
 * active. The shield drops for 3 seconds after it fires a shockwave, which is the player's
 * damage window - this is the whole encounter loop.
 *
 * <h2>Counter</h2>
 * {@link #beginShockwave()} launches a linear shockwave along its facing after a short
 * telegraph, damaging and knocking back everything in a 12-block corridor.
 */
public class VoidSentinelEntity extends HostileEntity {

    private static final float PROJECTILE_ABSORPTION = 0.8f;
    private static final int SHOCKWAVE_COOLDOWN = 160;
    private static final int SHOCKWAVE_CHARGE = 18;
    private static final int SHIELD_DOWN_AFTER_FIRE = 60;

    private boolean shieldActive = true;
    private int shockwaveCooldown;
    private int shockwaveCharge = -1;
    private int shieldDownTicks;

    public VoidSentinelEntity(EntityType<? extends VoidSentinelEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 25;
    }

    public static DefaultAttributeContainer.Builder createSentinelAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(2, new SentinelShockwaveGoal(this));
        this.goalSelector.add(5, new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.9));
        this.goalSelector.add(6, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 20.0f));
        this.goalSelector.add(7, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.RevengeGoal(this));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (shockwaveCooldown > 0) {
            shockwaveCooldown--;
        }
        if (shieldDownTicks > 0) {
            shieldDownTicks--;
            if (shieldDownTicks == 0) {
                shieldActive = true;
                playShieldSound(SoundEvents.ITEM_SHIELD_BLOCK);
            }
        }
        if (shockwaveCharge >= 0) {
            shockwaveCharge--;
            if (shockwaveCharge == 0) {
                fireShockwave();
                shockwaveCharge = -1;
            }
        }
    }

    public boolean isShieldActive() {
        return shieldActive;
    }

    public void beginShockwave() {
        if (shockwaveCharge < 0 && shockwaveCooldown <= 0) {
            shockwaveCharge = SHOCKWAVE_CHARGE;
            shieldActive = false;
            playShieldSound(SoundEvents.BLOCK_BEACON_ACTIVATE);
        }
    }

    private void playShieldSound(SoundEvent sound) {
        this.getWorld().playSound(null, this.getBlockPos(), sound,
                net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 0.7f);
    }

    private void fireShockwave() {
        shockwaveCooldown = SHOCKWAVE_COOLDOWN;
        shieldDownTicks = SHIELD_DOWN_AFTER_FIRE;
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        Vec3d origin = this.getPos().add(0, this.getHeight() * 0.5, 0);
        Vec3d facing = this.getRotationVec(1.0f).normalize();
        Vec3d sweep = origin.add(facing.multiply(6.0));
        List<LivingEntity> victims = serverWorld.getEntitiesByClass(LivingEntity.class,
                new net.minecraft.util.math.Box(origin, origin.add(facing.multiply(13.0))).expand(2.5),
                e -> e != this && e.isAlive() && !(e instanceof VoidSentinelEntity));
        for (LivingEntity victim : victims) {
            Vec3d toVictim = victim.getPos().subtract(origin);
            double forward = toVictim.dotProduct(facing);
            if (forward < 1.0 || forward > 13.0) {
                continue;
            }
            victim.damage(this.getDamageSources().mobAttack(this), 7.0f);
            victim.addVelocity(facing.x * 1.2, 0.45, facing.z * 1.2);
            if (victim instanceof PlayerEntity player) {
                player.velocityModified = true;
            }
        }
        serverWorld.playSound(null, this.getBlockPos(), ModSounds.SPELL_NOX,
                net.minecraft.sound.SoundCategory.HOSTILE, 1.5f, 0.6f);
        serverWorld.spawnParticles(ModParticles.VOID_MOTE, sweep.x, sweep.y, sweep.z, 80, 1.5, 1.5, 1.5, 0.2);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (shieldActive && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            float absorbed = amount * PROJECTILE_ABSORPTION;
            if (!this.getWorld().isClient) {
                playShieldSound(SoundEvents.ITEM_SHIELD_BLOCK);
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                            this.getX(), this.getBodyY(0.6), this.getZ(), 24, 0.6, 0.8, 0.6, 0.1);
                }
            }
            return super.damage(source, amount - absorbed);
        }
        return super.damage(source, amount);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("ShieldActive", shieldActive);
        nbt.putInt("ShockwaveCooldown", shockwaveCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        shieldActive = nbt.getBoolean("ShieldActive");
        shockwaveCooldown = nbt.getInt("ShockwaveCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WARDEN_DEATH;
    }

    /** Fires the shockwave when the target sits in the ideal 4-12 block band. */
    static final class SentinelShockwaveGoal extends net.minecraft.entity.ai.goal.Goal {
        private final VoidSentinelEntity sentinel;

        SentinelShockwaveGoal(VoidSentinelEntity sentinel) {
            this.sentinel = sentinel;
            this.setControls(java.util.EnumSet.of(net.minecraft.entity.ai.goal.Goal.Control.MOVE,
                    net.minecraft.entity.ai.goal.Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = sentinel.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distance = sentinel.squaredDistanceTo(target);
            return distance > 16.0 && distance < 144.0;
        }

        @Override
        public boolean shouldContinue() {
            return sentinel.getTarget() != null && sentinel.getTarget().isAlive();
        }

        @Override
        public void start() {
            sentinel.getNavigation().stop();
            sentinel.beginShockwave();
        }

        @Override
        public void tick() {
            LivingEntity target = sentinel.getTarget();
            if (target != null) {
                sentinel.getLookControl().lookAt(target, 30f, 30f);
            }
        }
    }
}
