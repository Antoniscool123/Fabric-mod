package com.aetherius.entity;

import com.aetherius.api.magic.SpellSchool;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Regional mini-boss framework: one entity class, four variants
 * (Sol / Luna / Terra / Caelum). Each Wardens guards a regional temple and drops the
 * Elemental Heart that sockets into a Sky-Rift anchor (Act III keys).
 *
 * <p>Splitting the four Wardens by variant instead of four subclasses keeps the AI in one
 * place while still giving each one a distinct signature attack curve.</p>
 */
public class ElementalWardenEntity extends HostileEntity {

    public enum Variant {
        SOL(SpellSchool.SOL, "heart_of_sol", 1.0f),
        LUNA(SpellSchool.LUNA, "heart_of_luna", 0.9f),
        TERRA(SpellSchool.TERRA, "heart_of_terra", 1.2f),
        CAELUM(SpellSchool.CAELUM, "heart_of_caelum", 0.8f);

        private final SpellSchool school;
        private final String heartPath;
        private final float castScale;

        Variant(SpellSchool school, String heartPath, float castScale) {
            this.school = school;
            this.heartPath = heartPath;
            this.castScale = castScale;
        }

        public SpellSchool school() {
            return school;
        }
    }

    private Variant variant;
    private int abilityCooldown = 100;

    public ElementalWardenEntity(EntityType<? extends ElementalWardenEntity> type, World world, Variant variant) {
        super(type, world);
        this.variant = variant;
        this.experiencePoints = 120;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createWardenAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 180.0)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    public Variant variant() {
        return variant;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.8));
        this.goalSelector.add(4, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 32f));
        this.goalSelector.add(5, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.RevengeGoal(this));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (abilityCooldown > 0) {
            abilityCooldown--;
        } else {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.squaredDistanceTo(target) < 400.0) {
                useSignatureAbility(target);
                abilityCooldown = (int) (200 * variant.castScale);
            }
        }
    }

    private void useSignatureAbility(LivingEntity target) {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        net.minecraft.sound.SoundEvent castSound = switch (variant) {
            case SOL -> ModSounds.SPELL_SOL;
            case LUNA -> ModSounds.SPELL_LUNA;
            case TERRA -> ModSounds.SPELL_TERRA;
            case CAELUM -> ModSounds.SPELL_CAELUM;
        };
        world.playSound(null, this.getBlockPos(), castSound, bossSoundCategory(), 1.4f, 0.8f);
        switch (variant) {
            case SOL -> {
                target.setOnFireFor(6);
                target.damage(this.getDamageSources().magic(), 8.0f);
                world.spawnParticles(ModParticles.SOL_EMBER, target.getX(), target.getBodyY(0.5), target.getZ(),
                        60, 1.2, 1.2, 1.2, 0.1);
            }
            case LUNA -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 3));
                target.setFrozenTicks(240);
                this.heal(12.0f);
                world.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE,
                        this.getX(), this.getBodyY(0.6), this.getZ(), 70, 1.2, 1.4, 1.2, 0.05);
            }
            case TERRA -> {
                Vec3d push = target.getPos().subtract(this.getPos()).normalize().multiply(1.4);
                target.addVelocity(push.x, 0.6, push.z);
                if (target instanceof PlayerEntity player) {
                    player.velocityModified = true;
                }
                target.damage(this.getDamageSources().magic(), 6.0f);
                for (BlockPos pos : BlockPos.iterateOutwards(this.getBlockPos(), 4, 2, 4)) {
                    if (world.getBlockState(pos).isAir() && world.random.nextFloat() < 0.12f) {
                        world.setBlockState(pos, net.minecraft.block.Blocks.DEEPSLATE.getDefaultState());
                    }
                }
            }
            case CAELUM -> {
                net.minecraft.entity.LightningEntity bolt =
                        EntityType.LIGHTNING_BOLT.create(world);
                if (bolt != null) {
                    bolt.refreshPositionAfterTeleport(target.getPos());
                    bolt.setChanneler(target instanceof ServerPlayerEntity channeler ? channeler : null);
                    world.spawnEntity(bolt);
                }
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 40, 0));
            }
        }
    }

    private net.minecraft.sound.SoundCategory bossSoundCategory() {
        return net.minecraft.sound.SoundCategory.HOSTILE;
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        if (causedByPlayer) {
            Item heart = switch (variant) {
                case SOL -> ModItems.HEART_OF_SOL;
                case LUNA -> ModItems.HEART_OF_LUNA;
                case TERRA -> ModItems.HEART_OF_TERRA;
                case CAELUM -> ModItems.HEART_OF_CAELUM;
            };
            this.dropStack(new ItemStack(heart));
            if (source.getAttacker() instanceof PlayerEntity player) {
                player.sendMessage(Text.translatable("aetherius.warden.heart_dropped",
                        Text.translatable(heart.getTranslationKey()).formatted(Formatting.LIGHT_PURPLE)), false);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("Variant", variant.name());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Variant")) {
            try {
                variant = Variant.valueOf(nbt.getString("Variant"));
            } catch (IllegalArgumentException ignored) {
                variant = Variant.SOL;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }
}
