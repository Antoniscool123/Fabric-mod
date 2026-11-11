package com.aetherius.api.magic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * A single inscribable rune. Runes are the alphabet of the magic system: three runes
 * inscribed in order onto a wand form a {@link com.aetherius.api.spell.RuneCombination}
 * which {@link com.aetherius.api.spell.SpellRegistry} resolves into a spell.
 *
 * <p>Cost design: tier I runes are cheap and always available; tier II unlock at
 * school mastery 2 (Chronicle Act II); tier III requires the Observatory telescope
 * repair plus the matching moon phase (see {@code TelescopeBlockEntity#currentRunes}).</p>
 */
public enum Rune {
    // ---- Sol: Pyromancy -------------------------------------------------
    IGNIS(SpellSchool.SOL, 1, 8f, 1f, "ember"),
    FLARIS(SpellSchool.SOL, 2, 18f, 3f, "flare"),
    SOLARIS(SpellSchool.SOL, 3, 34f, 6f, "sunfire"),

    // ---- Luna: Aquamancy ------------------------------------------------
    UNDA(SpellSchool.LUNA, 1, 7f, 0f, "tide"),
    GELU(SpellSchool.LUNA, 2, 16f, 0f, "frost"),
    LUNARIS(SpellSchool.LUNA, 3, 30f, 0f, "moonwell"),

    // ---- Terra: Terramancy ----------------------------------------------
    LAPIS(SpellSchool.TERRA, 1, 9f, 0f, "stone"),
    MURUS(SpellSchool.TERRA, 2, 20f, 1f, "wall"),
    TERRANOX(SpellSchool.TERRA, 3, 32f, 4f, "deepstone"),

    // ---- Caelum: Aeromancy ----------------------------------------------
    VENTUS(SpellSchool.CAELUM, 1, 6f, 1f, "gale"),
    FULGUR(SpellSchool.CAELUM, 2, 19f, 3f, "lightning"),
    CAELUS(SpellSchool.CAELUM, 3, 28f, 5f, "skybreak"),

    // ---- Nox: Voidmancy --------------------------------------------------
    UMBRA(SpellSchool.NOX, 1, 10f, 12f, "shadow"),
    HIATUS(SpellSchool.NOX, 2, 22f, 26f, "rift"),
    ECLIPSE(SpellSchool.NOX, 3, 36f, 42f, "eclipse");

    private final SpellSchool school;
    private final int tier;
    private final float manaCost;
    private final float tensionCost;
    private final String translationKey;

    Rune(SpellSchool school, int tier, float manaCost, float tensionCost, String key) {
        this.school = school;
        this.tier = tier;
        this.manaCost = manaCost;
        this.tensionCost = tensionCost;
        this.translationKey = "aetherius.rune." + key;
    }

    public SpellSchool school() {
        return school;
    }

    /** 1..3 - gates availability behind Chronicle progress and moon phases. */
    public int tier() {
        return tier;
    }

    public float manaCost() {
        return manaCost;
    }

    /** Astral Tension added on cast. Only Nox (and hybrid) runes are non-zero by default. */
    public float tensionCost() {
        return tensionCost;
    }

    public String translationKey() {
        return translationKey;
    }

    public static Set<Rune> of(SpellSchool school) {
        Set<Rune> out = EnumSet.noneOf(Rune.class);
        for (Rune rune : values()) {
            if (rune.school == school) {
                out.add(rune);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    public static Rune byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
