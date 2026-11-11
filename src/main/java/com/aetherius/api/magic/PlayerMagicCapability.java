package com.aetherius.api.magic;

import com.aetherius.Aetherius;
import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.spell.AbstractSpell;
import com.aetherius.api.spell.MagicCosts;
import com.aetherius.api.spell.RuneCombination;
import com.aetherius.api.spell.SpellContext;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.network.AetherNetworking;
import com.aetherius.registry.ModBlocks;
import com.aetherius.registry.ModEntities;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import com.aetherius.registry.ModStatusEffects;
import com.aetherius.world.blight.CelestialBlightManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative per-player magic state.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Created lazily by {@code PlayerEntityMixin#aetherius$getMagic()} on both sides.</li>
 *   <li>Server copy ticks from {@link com.aetherius.Aetherius} server tick events.</li>
 *   <li>Client copy is a read-only mirror overwritten by {@code MagicSyncS2CPacket}.</li>
 *   <li>Serialised into the player's custom NBT and copied on respawn/dimension change.</li>
 * </ol>
 *
 * <h2>Resource rules</h2>
 * <ul>
 *   <li><b>Mana</b> regenerates passively and triples within 6 blocks of an Aether Fountain.</li>
 *   <li><b>Astral Tension</b> only rises from casts; decays slowly, faster while standing
 *       in the Starlit Meadows biome or drinking a Purified Starlight Elixir.</li>
 *   <li>At 50 tension the player enters <i>Mana Volatility</i> (damage x2, cost x2).</li>
 *   <li>At 100 tension an <b>Astral Backfire</b> detonates the pool, blinds the caster,
 *       spawns Void Specters and seeds Celestial Blight in the local chunk.</li>
 * </ul>
 */
public final class PlayerMagicCapability implements MagicUser {

    // ---- Tuning ----------------------------------------------------------
    public static final float BASE_MAX_MANA = 100f;
    public static final float MANA_REGEN_PER_SECOND = 2.0f;
    public static final float FOUNTAIN_REGEN_MULTIPLIER = 3.0f;
    public static final float TENSION_DECAY_PER_SECOND = 0.4f;
    public static final float VOLATILITY_THRESHOLD = 50f;
    public static final float BACKFIRE_THRESHOLD = 100f;
    public static final float VOLATILITY_DAMAGE_MULTIPLIER = 2.0f;
    public static final float VOLATILITY_COST_MULTIPLIER = 2.0f;
    public static final float POST_BACKFIRE_TENSION = 25f;
    public static final int CAST_LOCKOUT_TICKS = 8;
    public static final int BACKFIRE_DURATION_TICKS = 60;
    public static final int CELESTIAL_BLINDNESS_TICKS = 220;
    public static final int FOUNTAIN_SCAN_RADIUS = 6;

    public static final int NBT_VERSION = 2;

    private final PlayerEntity owner;

    private float mana = BASE_MAX_MANA;
    private float maxMana = BASE_MAX_MANA;
    private float astralTension;
    private float tensionResistance;
    private int castLockoutTicks;
    private int backfireTicks;
    private int celestialBlindnessTicks;
    private int gravityInversionTicks;
    private int elytraLockTicks;
    private int lifetimeTicks;
    private boolean nearFountain;
    private boolean dirty = true;

    private final EnumSet<Rune> unlockedRunes = EnumSet.noneOf(Rune.class);
    private final EnumMap<SpellSchool, Integer> mastery = new EnumMap<>(SpellSchool.class);
    private final Map<Identifier, Integer> cooldowns = new HashMap<>();
    private final PlayerChronicle chronicle = new PlayerChronicle();
    private final Deque<Long> castTimestamps = new ArrayDeque<>();

    public PlayerMagicCapability(PlayerEntity owner) {
        this.owner = owner;
        this.unlockedRunes.add(Rune.IGNIS);
        this.unlockedRunes.add(Rune.UNDA);
        this.unlockedRunes.add(Rune.LAPIS);
        this.unlockedRunes.add(Rune.VENTUS);
        this.unlockedRunes.add(Rune.UMBRA);
        for (SpellSchool school : SpellSchool.values()) {
            mastery.put(school, 1);
        }
    }

    // =====================================================================
    //  Tick pipeline (server authoritative only)
    // =====================================================================

    public void tick() {
        if (!(owner instanceof ServerPlayerEntity player)) {
            return; // client mirror never simulates
        }
        lifetimeTicks++;
        if (castLockoutTicks > 0) {
            castLockoutTicks--;
        }
        if (backfireTicks > 0) {
            backfireTicks--;
        }
        if (celestialBlindnessTicks > 0) {
            celestialBlindnessTicks--;
        }
        if (gravityInversionTicks > 0) {
            gravityInversionTicks--;
        }
        if (elytraLockTicks > 0) {
            elytraLockTicks--;
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
        }
        tickCooldowns();

        if (lifetimeTicks % 5 == 0) {
            nearFountain = scanForFountain(player);
        }
        if (lifetimeTicks % 20 == 0) {
            regenerateMana(player);
            decayTension(player);
        }
        if (lifetimeTicks % 40 == 0) {
            pruneCastHistory();
        }
    }

    private void tickCooldowns() {
        if (cooldowns.isEmpty()) {
            return;
        }
        cooldowns.entrySet().removeIf(entry -> {
            int left = entry.getValue() - 1;
            if (left <= 0) {
                return true;
            }
            entry.setValue(left);
            return false;
        });
    }

    private boolean scanForFountain(ServerPlayerEntity player) {
        BlockPos origin = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, FOUNTAIN_SCAN_RADIUS, 4, FOUNTAIN_SCAN_RADIUS)) {
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.isOf(ModBlocks.AETHER_FOUNTAIN)) {
                return true;
            }
        }
        return false;
    }

    private void regenerateMana(ServerPlayerEntity player) {
        float regen = MANA_REGEN_PER_SECOND;
        regen *= 1f + 0.15f * getSchoolMastery(SpellSchool.LUNA);
        if (nearFountain) {
            regen *= FOUNTAIN_REGEN_MULTIPLIER;
        }
        if (backfireTicks > 0) {
            regen *= 0.25f; // recovering from a backfire hurts
        }
        if (mana < maxMana) {
            mana = Math.min(maxMana, mana + regen);
            markDirty();
        }
    }

    private void decayTension(ServerPlayerEntity player) {
        if (astralTension <= 0f) {
            return;
        }
        float decay = TENSION_DECAY_PER_SECOND * (1f + tensionResistance);
        // Standing in a Starlit Meadow calms the echo: 4x faster decay.
        if (player.getWorld().getBiome(player.getBlockPos()).matchesKey(
                net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME,
                        com.aetherius.util.Aeth.id("starlit_meadows")))) {
            decay *= 4f;
        }
        astralTension = Math.max(0f, astralTension - decay);
        markDirty();
    }

    private void pruneCastHistory() {
        long cutoff = owner.getWorld().getTime() - 100L;
        while (!castTimestamps.isEmpty() && castTimestamps.peekFirst() < cutoff) {
            castTimestamps.pollFirst();
        }
    }

    // =====================================================================
    //  Casting
    // =====================================================================

    /**
     * Resolves and executes a rune sequence.
     *
     * @param combination ordered runes inscribed on the wand (1-3 runes)
     * @return structured result - never throws for gameplay-level failures
     */
    public CastResult cast(RuneCombination combination) {
        if (!(owner instanceof ServerPlayerEntity player)) {
            return CastResult.fail(CastResult.Reason.CLIENT_SIDE);
        }
        if (backfireTicks > 0) {
            return CastResult.fail(CastResult.Reason.BACKFIRING);
        }
        if (castLockoutTicks > 0) {
            return CastResult.fail(CastResult.Reason.ON_COOLDOWN);
        }
        AbstractSpell spell = SpellRegistry.resolve(combination);
        if (spell == null || !knowsAll(combination)) {
            return CastResult.fail(CastResult.Reason.UNKNOWN_COMBINATION);
        }
        if (cooldowns.getOrDefault(spell.id(), 0) > 0) {
            return CastResult.fail(CastResult.Reason.ON_COOLDOWN);
        }

        MagicCosts base = MagicCosts.of(combination);
        float manaCost = base.mana() * (isManaVolatile() ? VOLATILITY_COST_MULTIPLIER : 1f);
        if (mana + 0.001f < manaCost) {
            return CastResult.fail(CastResult.Reason.NOT_ENOUGH_MANA);
        }

        SpellContext context = new SpellContext(player, this, combination);
        if (!spell.canCast(context)) {
            return CastResult.fail(CastResult.Reason.SILENCED);
        }

        mana -= manaCost;
        castLockoutTicks = CAST_LOCKOUT_TICKS;
        cooldowns.put(spell.id(), Math.max(1, spell.cooldownTicks()));
        castTimestamps.addLast(player.getWorld().getTime());
        addAstralTension(base.tension());

        spell.cast(context);
        ChronicleManager.get().onSpellCast(player, spell, combination);
        markDirty();
        return CastResult.ok(spell.id(), manaCost, base.tension());
    }

    public boolean knowsAll(RuneCombination combination) {
        for (Rune rune : combination.runes()) {
            if (!unlockedRunes.contains(rune)) {
                return false;
            }
        }
        return true;
    }

    /** Number of successful casts in the last 5 seconds - used by Chronicle objectives. */
    public int recentCastCount() {
        return castTimestamps.size();
    }

    // =====================================================================
    //  Astral Tension
    // =====================================================================

    @Override
    public void addAstralTension(float amount) {
        if (amount <= 0f) {
            return;
        }
        astralTension = Mth.clamp(astralTension + amount * (1f - tensionResistance), 0f, 100f);
        if (astralTension >= BACKFIRE_THRESHOLD && backfireTicks <= 0) {
            triggerAstralBackfire();
        }
        markDirty();
    }

    /**
     * The 100-tension catastrophe. Public so bosses can force-trigger it
     * (Nox'Thal's Phase 3 "Eclipse Pulse" does exactly this).
     */
    public void triggerAstralBackfire() {
        if (!(owner instanceof ServerPlayerEntity player)) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        backfireTicks = BACKFIRE_DURATION_TICKS;
        celestialBlindnessTicks = CELESTIAL_BLINDNESS_TICKS;
        gravityInversionTicks = 0;
        mana = 0f;
        astralTension = POST_BACKFIRE_TENSION;
        castLockoutTicks = BACKFIRE_DURATION_TICKS;

        player.addStatusEffect(new StatusEffectInstance(ModStatusEffects.CELESTIAL_BLINDNESS,
                CELESTIAL_BLINDNESS_TICKS, 0, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.NAUSEA,
                160, 1, false, false, false));

        world.playSound(null, player.getBlockPos(), ModSounds.ASTRAL_BACKFIRE,
                SoundCategory.PLAYERS, 1.4f, 0.75f);
        world.spawnParticles(ModParticles.ASTRAL_SPARK,
                player.getX(), player.getBodyY(0.5), player.getZ(),
                140, 2.2, 2.2, 2.2, 0.28);
        world.createExplosion(player, player.getX(), player.getBodyY(0.5), player.getZ(),
                2.0f, net.minecraft.world.World.ExplosionSourceType.BLOCK);

        spawnVoidSpecters(player, world);
        CelestialBlightManager.get().infect(world, player.getBlockPos(), 3);
        AetherNetworking.sendScreenShake(world, player.getBlockPos(), 24.0, 30);

        Aetherius.LOGGER.warn("[Aetherius] Astral Backfire for {} at {} (tension reset to {})",
                player.getGameProfile().getName(), player.getBlockPos(), POST_BACKFIRE_TENSION);
        markDirty();
    }

    private void spawnVoidSpecters(ServerPlayerEntity player, ServerWorld world) {
        int count = 2 + world.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            EntityType<?> type = ModEntities.VOID_SPECTER;
            net.minecraft.entity.Entity entity = type.create(world);
            if (entity == null) {
                continue;
            }
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 2.5 + world.random.nextDouble() * 2.0;
            entity.refreshPositionAndAngles(
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.5,
                    player.getZ() + Math.sin(angle) * radius,
                    world.random.nextFloat() * 360f, 0f);
            if (entity instanceof net.minecraft.entity.mob.HostileEntity hostile) {
                hostile.setTarget(player);
            }
            world.spawnEntity(entity);
        }
    }

    /** Reduces future tension gain, awarded by the Purification rite. */
    public void addTensionResistance(float amount) {
        tensionResistance = Mth.clamp(tensionResistance + amount, 0f, 0.75f);
        markDirty();
    }

    public float getTensionResistance() {
        return tensionResistance;
    }

    // =====================================================================
    //  Boss-applied states
    // =====================================================================

    public void setGravityInversion(int ticks) {
        gravityInversionTicks = Math.max(gravityInversionTicks, ticks);
        markDirty();
    }

    public void setElytraLock(int ticks) {
        elytraLockTicks = Math.max(elytraLockTicks, ticks);
        markDirty();
    }

    public void setCelestialBlindness(int ticks) {
        celestialBlindnessTicks = Math.max(celestialBlindnessTicks, ticks);
        markDirty();
    }

    @Override
    public boolean isGravityInverted() {
        return gravityInversionTicks > 0;
    }

    @Override
    public boolean isElytraLocked() {
        return elytraLockTicks > 0;
    }

    @Override
    public boolean isBackfiring() {
        return backfireTicks > 0;
    }

    // =====================================================================
    //  Progression
    // =====================================================================

    public boolean unlockRune(Rune rune) {
        boolean added = unlockedRunes.add(rune);
        if (added) {
            markDirty();
        }
        return added;
    }

    public int advanceMastery(SpellSchool school) {
        int next = Math.min(5, getSchoolMastery(school) + 1);
        mastery.put(school, next);
        markDirty();
        return next;
    }

    /** Runes that become available at the player's current mastery levels. */
    public Set<Rune> availableAtCurrentMastery() {
        Set<Rune> out = EnumSet.noneOf(Rune.class);
        for (Rune rune : Rune.values()) {
            if (rune.tier() == 1 || mastery.getOrDefault(rune.school(), 0) >= rune.tier()) {
                out.add(rune);
            }
        }
        return out;
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    @Override
    public float getMana() {
        return mana;
    }

    @Override
    public void setMana(float mana) {
        this.mana = Mth.clamp(mana, 0f, maxMana);
        markDirty();
    }

    @Override
    public float getMaxMana() {
        return maxMana;
    }

    @Override
    public void setMaxMana(float maxMana) {
        this.maxMana = Math.max(10f, maxMana);
        this.mana = Math.min(this.mana, this.maxMana);
        markDirty();
    }

    @Override
    public void addMana(float amount) {
        setMana(mana + amount);
    }

    @Override
    public float getAstralTension() {
        return astralTension;
    }

    @Override
    public void setAstralTension(float tension) {
        astralTension = Mth.clamp(tension, 0f, 100f);
        markDirty();
    }

    @Override
    public boolean isManaVolatile() {
        return astralTension >= VOLATILITY_THRESHOLD;
    }

    @Override
    public int getBackfireTicks() {
        return backfireTicks;
    }

    @Override
    public int getCelestialBlindnessTicks() {
        return celestialBlindnessTicks;
    }

    @Override
    public int getSchoolMastery(SpellSchool school) {
        return mastery.getOrDefault(school, 1);
    }

    @Override
    public void setSchoolMastery(SpellSchool school, int level) {
        mastery.put(school, Mth.clamp(level, 0, 5));
        markDirty();
    }

    @Override
    public boolean knows(Rune rune) {
        return unlockedRunes.contains(rune);
    }

    @Override
    public Set<Rune> getUnlockedRunes() {
        return EnumSet.copyOf(unlockedRunes);
    }

    @Override
    public PlayerChronicle getChronicle() {
        return chronicle;
    }

    @Override
    public int getGravityInversionTicks() {
        return gravityInversionTicks;
    }

    @Override
    public int getElytraLockTicks() {
        return elytraLockTicks;
    }

    @Override
    public int getCastLockoutTicks() {
        return castLockoutTicks;
    }

    @Override
    public float getSpellDamageMultiplier() {
        return isManaVolatile() ? VOLATILITY_DAMAGE_MULTIPLIER : 1f;
    }

    public boolean isNearAetherFountain() {
        return nearFountain;
    }

    public PlayerEntity owner() {
        return owner;
    }

    // =====================================================================
    //  Sync support
    // =====================================================================

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    // =====================================================================
    //  Persistence
    // =====================================================================

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Version", NBT_VERSION);
        nbt.putFloat("Mana", mana);
        nbt.putFloat("MaxMana", maxMana);
        nbt.putFloat("AstralTension", astralTension);
        nbt.putFloat("TensionResistance", tensionResistance);

        NbtList runeList = new NbtList();
        for (Rune rune : unlockedRunes) {
            runeList.add(NbtString.of(rune.name()));
        }
        nbt.put("Runes", runeList);

        NbtCompound masteryNbt = new NbtCompound();
        mastery.forEach((school, level) -> masteryNbt.putInt(school.id(), level));
        nbt.put("Mastery", masteryNbt);

        NbtCompound chronicleNbt = new NbtCompound();
        chronicle.writeNbt(chronicleNbt);
        nbt.put("Chronicle", chronicleNbt);
        return nbt;
    }

    @Override
    public void fromNbt(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return;
        }
        maxMana = nbt.contains("MaxMana") ? nbt.getFloat("MaxMana") : BASE_MAX_MANA;
        mana = Mth.clamp(nbt.contains("Mana") ? nbt.getFloat("Mana") : maxMana, 0f, maxMana);
        astralTension = Mth.clamp(nbt.getFloat("AstralTension"), 0f, 100f);
        tensionResistance = nbt.getFloat("TensionResistance");

        if (nbt.contains("Runes", NbtElement.LIST_TYPE)) {
            unlockedRunes.clear();
            NbtList runeList = nbt.getList("Runes", NbtElement.STRING_TYPE);
            for (int i = 0; i < runeList.size(); i++) {
                Rune rune = Rune.byName(runeList.getString(i));
                if (rune != null) {
                    unlockedRunes.add(rune);
                }
            }
        }
        if (nbt.contains("Mastery", NbtElement.COMPOUND_TYPE)) {
            NbtCompound masteryNbt = nbt.getCompound("Mastery");
            for (SpellSchool school : SpellSchool.values()) {
                if (masteryNbt.contains(school.id())) {
                    mastery.put(school, masteryNbt.getInt(school.id()));
                }
            }
        }
        if (nbt.contains("Chronicle", NbtElement.COMPOUND_TYPE)) {
            chronicle.readNbt(nbt.getCompound("Chronicle"));
        }
        markDirty();
    }

    /** Called from {@code ServerPlayerEntityMixin#copyFrom} on respawn / dimension change. */
    public void copyFrom(PlayerMagicCapability old) {
        this.maxMana = old.maxMana;
        this.mana = Math.max(old.mana, maxMana * 0.5f);
        this.astralTension = Math.min(old.astralTension, VOLATILITY_THRESHOLD - 1f);
        this.tensionResistance = old.tensionResistance;
        this.unlockedRunes.clear();
        this.unlockedRunes.addAll(old.unlockedRunes);
        this.mastery.clear();
        this.mastery.putAll(old.mastery);
        this.chronicle.copyFrom(old.chronicle);
        this.cooldowns.clear();
        this.castTimestamps.clear();
        this.backfireTicks = 0;
        this.gravityInversionTicks = 0;
        this.elytraLockTicks = 0;
        this.celestialBlindnessTicks = 0;
        markDirty();
    }
}
