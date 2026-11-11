package com.aetherius.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Aether-Manta - the Shattered Sky's mount.
 *
 * <p>A passive flying ray. While a player is mounted the manta is controlled directly by
 * look direction (pitch steers vertical), with a lower top speed than the elytra but no
 * durability or firework cost - which makes it the intended transport for the floating
 * island archipelago.</p>
 */
public class AetherMantaEntity extends PathAwareEntity {

    private static final double RIDE_SPEED = 0.34;

    public AetherMantaEntity(EntityType<? extends AetherMantaEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.experiencePoints = 6;
    }

    public static DefaultAttributeContainer.Builder createMantaAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new com.aetherius.entity.ai.MantaDriftGoal(this));
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            // Gentle bobbing so the manta reads as "swimming" through the air.
            if (this.age % 40 == 0) {
                this.setVelocity(this.getVelocity().add(0, 0.02, 0));
            }
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isLogicalSideForUpdatingMovement() && this.getControllingPassenger() instanceof PlayerEntity rider) {
            Vec3d look = rider.getRotationVec(1.0f);
            double vertical = look.y * 0.7 + (rider.isSneaking() ? -0.25 : 0);
            this.setVelocity(look.x * RIDE_SPEED, vertical * RIDE_SPEED, look.z * RIDE_SPEED);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.91));
            this.setYaw(rider.getYaw());
            this.prevYaw = this.getYaw();
        } else {
            super.travel(movementInput);
            this.setVelocity(this.getVelocity().multiply(0.98));
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty() && !player.isSneaking() && !this.isBaby()) {
            if (!this.getWorld().isClient) {
                player.startRiding(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof PlayerEntity player ? player : super.getControllingPassenger();
    }

    @Override
    protected boolean canAddPassenger(net.minecraft.entity.Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PHANTOM_DEATH;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier,
                                    net.minecraft.entity.damage.DamageSource damageSource) {
        return false;
    }

}
