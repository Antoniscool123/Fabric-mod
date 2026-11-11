package com.aetherius.entity;

import com.aetherius.api.chronicle.ChronicleCounters;
import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.entity.boss.bt.BTStatus;
import com.aetherius.entity.boss.bt.BossBlackboard;
import com.aetherius.entity.boss.phase.BossPhase;
import com.aetherius.entity.boss.phase.GravityCollapsePhase;
import com.aetherius.entity.boss.phase.StarlitHeraldPhase;
import com.aetherius.entity.boss.phase.SupernovaPhase;
import com.aetherius.network.BossEventS2CPacket;
import com.aetherius.registry.ModEntities;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <b>Nox'Thal, The Eclipse Dragon</b> - the Shattered Sky's final boss.
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li>Three {@link BossPhase} objects own their own behaviour tree, boss-bar colour and
 *       enter/exit choreography.</li>
 *   <li>The entity itself only exposes <i>actions</i> ({@code actionSwoop()},
 *       {@code actionStarStrike()}, ...) that the trees call. That split keeps AI readable
 *       and makes phases unit-testable with a fake blackboard.</li>
 *   <li>World-level mechanics (gravity inversion, elytra suppression, darkness) live here
 *       and are pushed to clients through {@link BossEventS2CPacket}.</li>
 * </ul>
 *
 * <h2>Phase contract</h2>
 * <pre>
 * 100%-67%  The Starlit Herald   ranged-immune, swoops + Cosmic Fire
 *  67%-34%  Gravity Collapse     anchors must die, 20s gravity inversion, elytra locked
 *  34%-0%   Supernova            screen black, AOE star-strikes, elytra locked
 * </pre>
 */
public class NoxThalEntity extends PathAwareEntity implements GeoEntity {

    public static final double ARENA_RADIUS = 48.0;

    /** Animations authored in {@code animations/entity/nox_thal.animation.json}. */
    private static final RawAnimation FLIGHT =
            RawAnimation.begin().thenLoop("animation.nox_thal.flight");
    private static final RawAnimation HEAVY_FLIGHT =
            RawAnimation.begin().thenLoop("animation.nox_thal.heavy_flight");
    private static final RawAnimation GROUNDED =
            RawAnimation.begin().thenLoop("animation.nox_thal.grounded");
    private static final RawAnimation SUPERNOVA =
            RawAnimation.begin().thenLoop("animation.nox_thal.supernova");
    private static final RawAnimation ATTACK =
            RawAnimation.begin().thenPlay("animation.nox_thal.attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final int TRANSITION_INVULNERABILITY = 50;

    private final ServerBossBar bossBar;
    private final List<BossPhase> phases;
    private final BossBlackboard blackboard;
    private final List<GravityAnchorEntity> anchors = new ArrayList<>();
    private final List<StarStrike> pendingStrikes = new ArrayList<>();

    private int phaseIndex;
    private int transitionTicks;
    private boolean rangedImmune;
    private boolean grounded;
    private boolean supernovaActive;
    private int supernovaTicks;
    private int breathCooldown;

    public NoxThalEntity(EntityType<? extends NoxThalEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.experiencePoints = 500;
        this.phases = List.of(new StarlitHeraldPhase(), new GravityCollapsePhase(), new SupernovaPhase());
        this.blackboard = new BossBlackboard(this);
        this.bossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        this.bossBar.setDarkenSky(true);
        this.setPersistent();
    }

    // =====================================================================
    //  GeckoLib animation
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 4-tick transitions keep phase changes from snapping.
        controllers.add(new AnimationController<>(this, "main", 4, this::animationPredicate)
                .triggerableAnim("attack", ATTACK));
    }

    private <E extends GeoAnimatable> PlayState animationPredicate(AnimationState<E> state) {
        if (!(state.getAnimatable() instanceof NoxThalEntity entity)) {
            return PlayState.STOP;
        }
        RawAnimation animation;
        if (entity.isSupernovaActive()) {
            animation = SUPERNOVA;
        } else if (entity.isGrounded()) {
            animation = GROUNDED;
        } else if (entity.phaseIndex() == 1) {
            animation = HEAVY_FLIGHT;
        } else {
            animation = FLIGHT;
        }
        return state.setAndContinue(animation);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public double getTick(Object object) {
        return this.age;
    }

    /** Called when the dragon bites or breathes, so the animation matches the hit. */
    public void playAttackAnimation() {
        this.triggerAnim("main", "attack");
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        // The dragon is a 1.6x-scaled model: give it a matching hitbox so nameplate, shadow and
        // hit tests line up with what the player sees.
        return EntityDimensions.changing(3.0f, 3.2f);
    }

    public static DefaultAttributeContainer.Builder createNoxThalAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 900.0)
                .add(EntityAttributes.GENERIC_ARMOR, 20.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 26.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 80.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        // Movement is handled by the phase trees; only targeting is vanilla-driven.
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.RevengeGoal(this));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                this, PlayerEntity.class, true));
    }

    // =====================================================================
    //  Tick pipeline
    // =====================================================================

    @Override
    protected void mobTick() {
        super.mobTick();
        blackboard.refresh();

        if (transitionTicks > 0) {
            transitionTicks--;
            this.setVelocity(this.getVelocity().multiply(0.6));
            return;
        }

        BossPhase phase = currentPhase();
        phase.tick(this);
        BTStatus status = phase.tree().tick(blackboard);
        if (status == BTStatus.RUNNING) {
            // Trees may report RUNNING for multi-tick actions; nothing extra needed.
            this.hoverTowardTarget();
        } else {
            this.hoverTowardTarget();
        }

        if (breathCooldown > 0) {
            breathCooldown--;
        }
        tickSupernova();
        resolvePendingStrikes();
        checkPhaseTransition();
        updateBossBar();
    }

    private void hoverTowardTarget() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.setVelocity(this.getVelocity().multiply(0.9));
            this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
            return;
        }
        Vec3d desired = new Vec3d(target.getX(), target.getY() + 9.0, target.getZ());
        Vec3d dir = desired.subtract(this.getPos());
        if (dir.lengthSquared() > 4.0) {
            this.setVelocity(this.getVelocity().add(dir.normalize().multiply(0.045)).multiply(0.94));
        }
        this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());
        this.getLookControl().lookAt(target, 30f, 30f);
    }

    private void checkPhaseTransition() {
        float ratio = this.getHealth() / this.getMaxHealth();
        int targetPhase = ratio <= 0.34f ? 2 : ratio <= 0.67f ? 1 : 0;
        if (targetPhase > phaseIndex && transitionTicks == 0) {
            setPhase(targetPhase);
        }
    }

    public void setPhase(int index) {
        if (index <= phaseIndex || index >= phases.size()) {
            return;
        }
        currentPhase().onExit(this);
        phaseIndex = index;
        transitionTicks = TRANSITION_INVULNERABILITY;
        BossPhase phase = currentPhase();
        phase.onEnter(this);
        applyBarColor(phase);
        sendBossEvent(BossEventS2CPacket.Kind.PHASE_CHANGE, 80);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, TRANSITION_INVULNERABILITY, 4,
                false, false, false));
    }

    private void applyBarColor(BossPhase phase) {
        this.bossBar.setColor(switch (phase.index()) {
            case 1 -> BossBar.Color.BLUE;
            case 2 -> BossBar.Color.RED;
            default -> BossBar.Color.PURPLE;
        });
        this.bossBar.setStyle(BossBar.Style.PROGRESS);
        this.bossBar.setName(Text.translatable(phase.titleKey()));
    }

    private void updateBossBar() {
        this.bossBar.setPercent(Math.max(0f, this.getHealth() / this.getMaxHealth()));
    }

    // =====================================================================
    //  Damage rules
    // =====================================================================

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient) {
            return super.damage(source, amount);
        }
        if (transitionTicks > 0) {
            deflect(source, "aetherius.boss.nox_thal.invulnerable");
            return false;
        }
        if (rangedImmune && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            deflect(source, "aetherius.boss.nox_thal.ranged_immune");
            playBossSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 0.6f);
            return false;
        }
        if (phaseIndex == 1 && !grounded) {
            deflect(source, "aetherius.boss.nox_thal.anchored");
            return false;
        }
        return super.damage(source, amount);
    }

    private void deflect(DamageSource source, String messageKey) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getBodyY(0.5), this.getZ(),
                    20, 1.5, 1.5, 1.5, 0.1);
        }
        if (source.getAttacker() instanceof ServerPlayerEntity player && this.age % 20 == 0) {
            player.sendMessage(Text.translatable(messageKey).formatted(net.minecraft.util.Formatting.GRAY), true);
        }
    }

    // =====================================================================
    //  Actions called by the behaviour trees
    // =====================================================================

    public BTStatus actionSwoop() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return BTStatus.FAILURE;
        }
        Vec3d dir = target.getPos().add(0, 0.5, 0).subtract(this.getPos()).normalize();
        this.setVelocity(dir.multiply(1.35).add(0, -0.15, 0));
        this.getLookControl().lookAt(target, 40f, 40f);
        if (this.squaredDistanceTo(target) < 9.0) {
            target.damage(this.getDamageSources().mobAttack(this), 12.0f);
            target.addVelocity(dir.x * 0.8, 0.5, dir.z * 0.8);
            if (target instanceof PlayerEntity player) {
                player.velocityModified = true;
            }
        }
        playBossSound(ModSounds.BOSS_NOX_ROAR, 1.0f, 1.3f);
        particlesAt(ModParticles.VOID_MOTE, this.getPos().add(0, 3, 0), 40, 2.0, 0.1);
        return BTStatus.SUCCESS;
    }

    public BTStatus actionCosmicBreath() {
        LivingEntity target = this.getTarget();
        if (target == null || breathCooldown > 0) {
            return BTStatus.FAILURE;
        }
        breathCooldown = 60;
        // Fire the head-snap animation for nearby clients (no-op on the server).
        this.triggerAnim("main", "attack");
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return BTStatus.FAILURE;
        }
        Vec3d dir = target.getPos().subtract(this.getPos()).normalize();
        Vec3d mouth = this.getPos().add(0, 4.2, 0).add(dir.multiply(3.0));
        for (int i = 0; i < 3; i++) {
            net.minecraft.entity.AreaEffectCloudEntity cloud =
                    new net.minecraft.entity.AreaEffectCloudEntity(serverWorld,
                            mouth.x + dir.x * i * 2.0, mouth.y + dir.y * i * 2.0, mouth.z + dir.z * i * 2.0);
            cloud.setRadius(2.6f);
            cloud.setRadiusGrowth(0.05f);
            cloud.setDuration(140);
            cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
            cloud.setColor(0x9B4DFF);
            cloud.setOwner(this);
            cloud.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1));
            cloud.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
            serverWorld.spawnEntity(cloud);
        }
        serverWorld.playSound(null, this.getBlockPos(), ModSounds.SPELL_NOX, SoundCategory.HOSTILE, 2.0f, 0.5f);
        return BTStatus.SUCCESS;
    }

    public BTStatus actionReposition(double hoverHeight) {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return BTStatus.FAILURE;
        }
        Vec3d desired = target.getPos().add(0, hoverHeight, 0);
        Vec3d dir = desired.subtract(this.getPos());
        if (dir.lengthSquared() > 1.0) {
            this.setVelocity(dir.normalize().multiply(0.55));
        }
        return BTStatus.SUCCESS;
    }

    public BTStatus actionGroundSlam() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return BTStatus.FAILURE;
        }
        this.setVelocity(0, -1.1, 0);
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 12, 3, 1, 3, 0.0);
        for (LivingEntity victim : nearbyLiving(8.0)) {
            victim.damage(this.getDamageSources().mobAttack(this), 16.0f);
            victim.addVelocity(0, 0.9, 0);
            if (victim instanceof PlayerEntity player) {
                player.velocityModified = true;
                magicOf(player).setGravityInversion(60);
            }
        }
        serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 2.0f, 0.6f);
        return BTStatus.SUCCESS;
    }

    public BTStatus actionTailSweep() {
        for (LivingEntity victim : nearbyLiving(9.0)) {
            Vec3d push = victim.getPos().subtract(this.getPos()).normalize().multiply(1.6);
            victim.addVelocity(push.x, 0.4, push.z);
            victim.damage(this.getDamageSources().mobAttack(this), 10.0f);
            if (victim instanceof PlayerEntity player) {
                player.velocityModified = true;
            }
        }
        particlesAt(ModParticles.ASTRAL_SPARK, this.getPos().add(0, 1.5, 0), 80, 6.0, 0.2);
        return BTStatus.SUCCESS;
    }

    public BTStatus actionMeleeLunge() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return BTStatus.FAILURE;
        }
        this.setVelocity(this.getPos().subtract(target.getPos()).normalize().multiply(-0.9));
        target.damage(this.getDamageSources().mobAttack(this), 14.0f);
        return BTStatus.SUCCESS;
    }

    /** Phase 3 signature: telegraphed AOE strikes around every player and every rune lantern. */
    public BTStatus actionStarStrike() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return BTStatus.FAILURE;
        }
        List<Vec3d> spots = new ArrayList<>();
        for (PlayerEntity player : serverWorld.getPlayers()) {
            if (player.squaredDistanceTo(this) < ARENA_RADIUS * ARENA_RADIUS) {
                spots.add(player.getPos());
            }
        }
        // Strike the rune lanterns too, so "safe" navigation aids become contested.
        for (BlockPos pos : BlockPos.iterateOutwards(this.getBlockPos(), 24, 12, 24)) {
            if (serverWorld.getBlockState(pos).isOf(com.aetherius.registry.ModBlocks.RUNE_LANTERN)) {
                spots.add(Vec3d.ofCenter(pos));
            }
        }
        int strikes = Math.min(6, Math.max(2, spots.size()));
        for (int i = 0; i < strikes; i++) {
            Vec3d spot = spots.get(serverWorld.random.nextInt(spots.size()));
            pendingStrikes.add(new StarStrike(spot, 26));
            serverWorld.spawnParticles(ModParticles.STAR_DUST, spot.x, spot.y + 12, spot.z, 40, 0.3, 6.0, 0.3, 0.05);
        }
        playBossSound(ModSounds.STARFALL, 1.6f, 0.8f);
        return BTStatus.SUCCESS;
    }

    private void resolvePendingStrikes() {
        if (pendingStrikes.isEmpty() || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        Iterator<StarStrike> iterator = pendingStrikes.iterator();
        while (iterator.hasNext()) {
            StarStrike strike = iterator.next();
            if (--strike.delayTicks > 0) {
                continue;
            }
            iterator.remove();
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                    strike.pos.x, strike.pos.y, strike.pos.z, 2, 0.5, 0.5, 0.5, 0.0);
            serverWorld.playSound(null, BlockPos.ofFloored(strike.pos), SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.HOSTILE, 1.4f, 1.2f);
            for (LivingEntity victim : serverWorld.getEntitiesByClass(LivingEntity.class,
                    new net.minecraft.util.math.Box(strike.pos, strike.pos).expand(2.6),
                    e -> e != this && e.isAlive())) {
                victim.damage(serverWorld.getDamageSources().magic(), 9.0f);
                victim.setOnFireFor(3);
            }
        }
    }

    // =====================================================================
    //  Arena mechanics
    // =====================================================================

    public void summonGravityAnchors(int count) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        anchors.removeIf(anchor -> anchor == null || anchor.isRemoved() || !anchor.isAlive());
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i;
            BlockPos spawn = this.getBlockPos().add(
                    (int) Math.round(Math.cos(angle) * 14), 0, (int) Math.round(Math.sin(angle) * 14));
            BlockPos ground = serverWorld.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, spawn);
            GravityAnchorEntity anchor = ModEntities.GRAVITY_ANCHOR.create(serverWorld);
            if (anchor == null) {
                continue;
            }
            anchor.refreshPositionAndAngles(ground.getX() + 0.5, ground.getY() + 3.0, ground.getZ() + 0.5,
                    0f, 0f);
            anchor.setOwner(this);
            serverWorld.spawnEntity(anchor);
            anchors.add(anchor);
            serverWorld.spawnParticles(ModParticles.VOID_MOTE,
                    anchor.getX(), anchor.getY(), anchor.getZ(), 60, 1.0, 1.0, 1.0, 0.2);
        }
        grounded = anchors.isEmpty();
        announce(Text.translatable("aetherius.boss.nox_thal.anchors_summoned"));
    }

    public boolean hasActiveAnchors() {
        anchors.removeIf(anchor -> anchor == null || anchor.isRemoved() || !anchor.isAlive());
        return !anchors.isEmpty();
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void onAnchorDestroyed(GravityAnchorEntity anchor) {
        anchors.remove(anchor);
        if (!hasActiveAnchors() && !grounded) {
            grounded = true;
            announce(Text.translatable("aetherius.boss.nox_thal.grounded")
                    .formatted(net.minecraft.util.Formatting.GOLD));
            playBossSound(ModSounds.BOSS_NOX_ROAR, 2.0f, 0.5f);
            sendBossEvent(BossEventS2CPacket.Kind.GRAVITY_PULSE, 40);
            if (this.getServer() != null) {
                for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) this.getWorld(),
                        this.getPos(), ARENA_RADIUS)) {
                    ChronicleManager.get().onFlag(player, ChronicleCounters.FLAG_ANCHOR_GROUNDED);
                }
            }
        }
    }

    public void invertGravityArena(int durationTicks) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, this.getPos(), ARENA_RADIUS)) {
            var magic = MagicComponents.get(player);
            magic.setGravityInversion(durationTicks);
            com.aetherius.network.AetherNetworking.syncMagic(player, magic);
            player.sendMessage(Text.translatable("aetherius.boss.nox_thal.gravity_flip")
                    .formatted(net.minecraft.util.Formatting.AQUA), true);
        }
        sendBossEvent(BossEventS2CPacket.Kind.GRAVITY_PULSE, durationTicks);
        playBossSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 0.5f);
    }

    public void applyElytraLockToArena(int ticks) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, this.getPos(), ARENA_RADIUS)) {
            var magic = MagicComponents.get(player);
            magic.setElytraLock(ticks);
            com.aetherius.network.AetherNetworking.syncMagic(player, magic);
        }
    }

    public void setElytraLockDuration(int ticks) {
        applyElytraLockToArena(ticks);
        sendBossEvent(BossEventS2CPacket.Kind.ELYTHERA_LOCK, ticks);
    }

    public void drainNearbyMana(double radius, float amount) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, this.getPos(), radius)) {
            var magic = MagicComponents.get(player);
            magic.setMana(Math.max(0f, magic.getMana() - amount));
            com.aetherius.network.AetherNetworking.syncMagic(player, magic);
        }
    }

    public void forceTensionSurgeArena() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, this.getPos(), ARENA_RADIUS)) {
            var magic = MagicComponents.get(player);
            magic.addAstralTension(8f);
            com.aetherius.network.AetherNetworking.syncMagic(player, magic);
        }
    }

    public void pulseAnchorGuidance() {
        for (GravityAnchorEntity anchor : anchors) {
            if (anchor != null && !anchor.isRemoved()) {
                particlesAt(ModParticles.STAR_DUST, anchor.getPos().add(0, 1, 0), 12, 0.6, 0.02);
            }
        }
    }

    // ---- Supernova ---------------------------------------------------------

    public void beginSupernova() {
        supernovaActive = true;
        supernovaTicks = 20 * 30;
        this.bossBar.setDarkenSky(true);
        refreshSupernovaDarkness();
    }

    public void refreshSupernovaDarkness() {
        sendBossEvent(BossEventS2CPacket.Kind.SUPERNOVA_DARKEN, supernovaTicks);
    }

    public void endSupernova() {
        supernovaActive = false;
        supernovaTicks = 0;
        sendBossEvent(BossEventS2CPacket.Kind.SUPERNOVA_END, 0);
        this.bossBar.setDarkenSky(false);
    }

    private void tickSupernova() {
        if (!supernovaActive) {
            return;
        }
        if (--supernovaTicks <= 0) {
            // Phase 3 never truly ends, but the darkness pulses in windows.
            supernovaTicks = 20 * 30;
            refreshSupernovaDarkness();
        }
    }

    public boolean isSupernovaActive() {
        return supernovaActive;
    }

    // =====================================================================
    //  Boss bar plumbing
    // =====================================================================

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            BossEventS2CPacket.send(serverWorld, this.getPos(), ARENA_RADIUS, this.getId(),
                    BossEventS2CPacket.Kind.PHASE_CHANGE, phaseIndex, 40);
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(net.minecraft.entity.Entity.RemovalReason reason) {
        this.bossBar.clearPlayers();
        super.remove(reason);
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    public BossPhase currentPhase() {
        return phases.get(Math.min(phaseIndex, phases.size() - 1));
    }

    public int phaseIndex() {
        return phaseIndex;
    }

    public int barColor() {
        return currentPhase().barColor();
    }

    public void setRangedImmune(boolean immune) {
        this.rangedImmune = immune;
    }

    public boolean isRangedImmune() {
        return rangedImmune;
    }

    public void announce(Text text) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        for (ServerPlayerEntity player : PlayerLookup.around(serverWorld, this.getPos(), 96)) {
            player.sendMessage(text, false);
        }
    }

    public void playBossSound(SoundEvent sound, float volume, float pitch) {
        this.getWorld().playSound(null, this.getBlockPos(), sound, SoundCategory.HOSTILE, volume, pitch);
    }

    /**
     * Overload for the newer vanilla sounds that are exposed as registry entries rather than
     * plain {@link SoundEvent} constants in 1.20.1.
     */
    public void playBossSound(RegistryEntry<SoundEvent> sound, float volume, float pitch) {
        playBossSound(sound.value(), volume, pitch);
    }

    private List<LivingEntity> nearbyLiving(double radius) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return List.of();
        }
        return serverWorld.getEntitiesByClass(LivingEntity.class,
                new net.minecraft.util.math.Box(this.getPos(), this.getPos()).expand(radius),
                e -> e != this && e.isAlive() && !(e instanceof NoxThalEntity));
    }

    private void particlesAt(net.minecraft.particle.ParticleEffect particle, Vec3d pos, int count,
                             double spread, double speed) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
        }
    }

    private static com.aetherius.api.magic.PlayerMagicCapability magicOf(PlayerEntity player) {
        return MagicComponents.get(player);
    }

    private void sendBossEvent(BossEventS2CPacket.Kind kind, int durationTicks) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            BossEventS2CPacket.send(serverWorld, this.getPos(), ARENA_RADIUS + 32, this.getId(), kind,
                    phaseIndex, durationTicks);
        }
    }

    // =====================================================================
    //  Death / persistence
    // =====================================================================

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            sendBossEvent(BossEventS2CPacket.Kind.BOSS_DEATH, 0);
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getBodyY(0.5), this.getZ(), 12, 4, 3, 4, 0.0);
            for (GravityAnchorEntity anchor : anchors) {
                if (anchor != null && !anchor.isRemoved()) {
                    anchor.discard();
                }
            }
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                ChronicleManager.get().onFlag(player, ChronicleCounters.FLAG_NOX_THAL_SLAIN);
                player.sendMessage(Text.translatable("aetherius.boss.nox_thal.defeated")
                        .formatted(net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD), false);
            }
        }
        this.dropStack(new net.minecraft.item.ItemStack(ModItems.AETHER_SHARD, 24));
        this.dropStack(new net.minecraft.item.ItemStack(ModItems.AETHER_BLOOD, 8));
        this.bossBar.clearPlayers();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Phase", phaseIndex);
        nbt.putBoolean("Grounded", grounded);
        nbt.putBoolean("Supernova", supernovaActive);
        nbt.putBoolean("RangedImmune", rangedImmune);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        phaseIndex = Math.min(nbt.getInt("Phase"), phases.size() - 1);
        grounded = nbt.getBoolean("Grounded");
        supernovaActive = nbt.getBoolean("Supernova");
        rangedImmune = nbt.getBoolean("RangedImmune");
        applyBarColor(currentPhase());
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BOSS_NOX_ROAR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDER_DRAGON_DEATH;
    }

    /** Pending telegraphed AOE. */
    private static final class StarStrike {
        private final Vec3d pos;
        private int delayTicks;

        private StarStrike(Vec3d pos, int delayTicks) {
            this.pos = pos;
            this.delayTicks = delayTicks;
        }
    }
}
