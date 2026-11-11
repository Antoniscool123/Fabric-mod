package com.aetherius.api.spell;

import com.aetherius.api.magic.SpellSchool;
import com.aetherius.util.Aeth;
import net.minecraft.util.Identifier;

/**
 * Base class for every castable spell.
 *
 * <p>Implementations are stateless singletons registered once into {@link SpellRegistry};
 * all per-cast data lives in the {@link SpellContext}. That makes spells cheap to
 * resolve, trivially thread-safe and easy to re-balance through the cost tables.</p>
 */
public abstract class AbstractSpell {

    private final Identifier id;
    private final String translationKey;
    private final SpellSchool primarySchool;
    private final SchoolPattern pattern;
    private final int cooldownTicks;

    protected AbstractSpell(String path, SpellSchool primarySchool, SchoolPattern pattern, int cooldownTicks) {
        this.id = Aeth.id(path);
        this.translationKey = "aetherius.spell." + path;
        this.primarySchool = primarySchool;
        this.pattern = pattern;
        this.cooldownTicks = cooldownTicks;
    }

    public Identifier id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public String descriptionKey() {
        return translationKey + ".desc";
    }

    public SpellSchool primarySchool() {
        return primarySchool;
    }

    public SchoolPattern pattern() {
        return pattern;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    /** Extra gating beyond mana/tension, e.g. dimension checks or boss suppression. */
    public boolean canCast(SpellContext context) {
        return true;
    }

    /** Executes the spell. Server side only - never call from a client tick. */
    public abstract void cast(SpellContext context);
}
