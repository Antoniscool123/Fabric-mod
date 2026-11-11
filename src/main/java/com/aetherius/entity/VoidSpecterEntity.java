package com.aetherius.entity;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.registry.ModParticles;
import com.aetherius.world.SkySpawnRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Void Specter - the consequence of an Astral Backfire.
 *
 * <p>Flying, relentless, and it drains <b>Astral Tension resistance</b> as much as health:
 * every successful hit saps mana and adds Tension, so a player already at 60+ tension who
 * gets swarmed can be pushed into a second backfire. That cascade is the intended
 * "void magic has a price" fantasy.</p>
 */
public class VoidSpecterEntity extends HostileEntity {

    private static final double CHASE_SPEED = 0.28;

    public VoidSpecterEntity(EntityType<? extends VoidSpecterEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createSpecterAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void initGoals() {
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.RevengeGoal(this));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 24f));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            Vec3d toTarget = new Vec3d(target.getX() - this.getX(),
                    (target.getBodyY(0.6)) - this.getY(),
                    target.getZ() - this.getZ());
            if (toTarget.lengthSquared() > 2.25) {
                Vec3d steer = toTarget.normalize().multiply(CHASE_SPEED);
                this.setVelocity(this.getVelocity().multiply(0.75).add(steer));
            } else {
                this.setVelocity(this.getVelocity().multiply(0.6));
            }
            this.getLookControl().lookAt(target, 30f, 30f);
        } else {
            // Idle drift: hold altitude over the island instead of sinking into the void for ever.
            double correction = SkySpawnRules.hoverCorrection(this.getWorld(), this.getBlockPos(), 9.0);
            this.setVelocity(this.getVelocity().multiply(0.94).add(0, correction, 0));
        }
        if (SkySpawnRules.isLostToVoid(this.getWorld(), this.getBlockPos())) {
            this.discard();
            return;
        }
        this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof PlayerEntity player && !this.getWorld().isClient) {
            var magic = MagicComponents.get(player);
            magic.setMana(Math.max(0f, magic.getMana() - 8f));
            magic.addAstralTension(6f);
            com.aetherius.network.AetherNetworking.syncMagic((net.minecraft.server.network.ServerPlayerEntity) player,
                    magic);
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ModParticles.VOID_MOTE, target.getX(), target.getBodyY(0.6),
                        target.getZ(), 26, 0.4, 0.6, 0.4, 0.08);
            }
        }
        return hit;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("NoGravity", true);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VEX_DEATH;
    }
}
