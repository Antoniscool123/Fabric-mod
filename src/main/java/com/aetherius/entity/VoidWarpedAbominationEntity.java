package com.aetherius.entity;

import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Void-Warped Abomination - a formerly passive mob consumed by Celestial Blight.
 *
 * <p>Created by {@code CelestialBlightManager} when blight reaches a passive animal: the
 * original entity is discarded, this takes its place, and the resulting abomination
 * inherits the animal's position and a permanent aggression toward players. Killing one is
 * the only way to reclaim a blighted farm.</p>
 */
public class VoidWarpedAbominationEntity extends HostileEntity {

    public VoidWarpedAbominationEntity(EntityType<? extends VoidWarpedAbominationEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createAbominationAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 32.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(4, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 16f));
        this.goalSelector.add(5, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.RevengeGoal(this));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                this, net.minecraft.entity.passive.PassiveEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.age % 30 == 0 && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ModParticles.BLIGHT_SPORE, this.getX(), this.getBodyY(0.7), this.getZ(),
                    6, 0.4, 0.6, 0.4, 0.01);
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        if (causedByPlayer) {
            this.dropStack(new ItemStack(ModItems.STAR_ASH_CLUMP, 1 + this.getRandom().nextInt(2)));
            if (this.getRandom().nextFloat() < 0.25f) {
                this.dropStack(new ItemStack(ModItems.VOID_DUST));
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_HOGLIN_ANGRY;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ZOMBIE_DEATH;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }
}
