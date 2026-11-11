package com.aetherius.entity;

import com.aetherius.registry.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Gravity Anchor - Nox'Thal Phase 2 objective.
 *
 * <p>Stationary, invulnerable to nothing but player damage, 60 HP. While any anchor is alive
 * the boss cannot be grounded (and therefore cannot be damaged in Phase 2). Every 4 seconds
 * an anchor pulses nearby players' gravity.</p>
 */
public class GravityAnchorEntity extends HostileEntity {

    private static final int PULSE_INTERVAL = 80;
    private static final int PULSE_INVERSION_TICKS = 70;

    private int pulseTimer = PULSE_INTERVAL;
    private NoxThalEntity owner;

    public GravityAnchorEntity(EntityType<? extends GravityAnchorEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.experiencePoints = 20;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAnchorAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    public void setOwner(NoxThalEntity owner) {
        this.owner = owner;
    }

    @Override
    protected void initGoals() {
        // Intentionally empty: anchors do not path or chase.
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (this.age % 20 == 0 && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ModParticles.VOID_MOTE, this.getX(), this.getBodyY(0.5), this.getZ(),
                    12, 0.6, 0.6, 0.6, -0.05);
        }
        if (--pulseTimer <= 0) {
            pulseTimer = PULSE_INTERVAL;
            pulse();
        }
        // Anchors slowly drift back to their spawn point so they cannot be kited away.
        if (this.age % 200 == 0) {
            this.setVelocity(0, 0, 0);
        }
    }

    private void pulse() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (PlayerEntity player : serverWorld.getEntitiesByClass(PlayerEntity.class,
                this.getBoundingBox().expand(9.0), PlayerEntity::isAlive)) {
            var magic = com.aetherius.api.magic.MagicComponents.get(player);
            magic.setGravityInversion(PULSE_INVERSION_TICKS);
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                com.aetherius.network.AetherNetworking.syncMagic(serverPlayer, magic);
            }
        }
        serverWorld.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                net.minecraft.sound.SoundCategory.HOSTILE, 1.2f, 0.6f);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Only player-driven damage counts; the boss fight must not be solved by mob infighting.
        if (!(source.getAttacker() instanceof PlayerEntity)) {
            amount *= 0.25f;
        }
        return super.damage(source, amount);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ModParticles.VOID_MOTE, this.getX(), this.getBodyY(0.5), this.getZ(),
                    120, 1.5, 1.5, 1.5, 0.3);
        }
        if (owner != null && !owner.isRemoved()) {
            owner.onAnchorDestroyed(this);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("PulseTimer", pulseTimer);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        pulseTimer = nbt.getInt("PulseTimer");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }
}
