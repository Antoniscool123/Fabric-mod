package com.aetherius.api.magic;

import com.aetherius.api.chronicle.PlayerChronicle;
import net.minecraft.nbt.NbtCompound;

import java.util.Set;

/**
 * Read/write contract for the player magic capability. Kept as an interface so the
 * HUD, spells, networking and tests can all depend on the contract instead of the
 * concrete implementation.
 */
public interface MagicUser {

    // ---- Mana -----------------------------------------------------------
    float getMana();

    void setMana(float mana);

    float getMaxMana();

    void setMaxMana(float maxMana);

    void addMana(float amount);

    // ---- Astral Tension --------------------------------------------------
    float getAstralTension();

    void setAstralTension(float tension);

    void addAstralTension(float amount);

    /** True at >= 50 tension: double spell damage, double spell cost. */
    boolean isManaVolatile();

    boolean isBackfiring();

    int getBackfireTicks();

    int getCelestialBlindnessTicks();

    // ---- Progression -----------------------------------------------------
    int getSchoolMastery(SpellSchool school);

    void setSchoolMastery(SpellSchool school, int level);

    boolean knows(Rune rune);

    boolean unlockRune(Rune rune);

    Set<Rune> getUnlockedRunes();

    PlayerChronicle getChronicle();

    // ---- Boss / dimension state -----------------------------------------
    boolean isGravityInverted();

    int getGravityInversionTicks();

    boolean isElytraLocked();

    int getElytraLockTicks();

    int getCastLockoutTicks();

    /** Multiplier applied to all outgoing spell damage (volatility aware). */
    float getSpellDamageMultiplier();

    // ---- Persistence -----------------------------------------------------
    NbtCompound toNbt();

    void fromNbt(NbtCompound nbt);
}
