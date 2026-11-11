package com.aetherius.api.spell;

import com.aetherius.api.magic.Rune;
import com.aetherius.api.spell.impl.BaseSpells;
import com.aetherius.api.spell.impl.DuoSpells;
import com.aetherius.api.spell.impl.ResonantEchoSpell;
import com.aetherius.api.spell.impl.TrioSpells;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central spell table: ordered school pattern -> {@link AbstractSpell}.
 *
 * <h2>Resolution order</h2>
 * <ol>
 *   <li>Exact ordered school pattern (the intended path).</li>
 *   <li>Unordered fallback: {@code Sol+Sol+Caelum} also satisfies {@code Caelum+Sol+Sol}
 *       if no exact entry exists - players who inscribe in a weird order still get
 *       something coherent.</li>
 *   <li>{@link ResonantEchoSpell} - a scaled generic effect so a cast is never "wasted"
 *       with zero feedback (it still costs mana and tension).</li>
 * </ol>
 *
 * <p>All registration is static and runs once from
 * {@link com.aetherius.Aetherius#onInitialize()} via {@link #bootstrap()}.</p>
 */
public final class SpellRegistry {

    private static final Map<String, AbstractSpell> BY_PATTERN = new LinkedHashMap<>();
    private static final Map<Identifier, AbstractSpell> BY_ID = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private SpellRegistry() {
    }

    /** Registers a spell. Later registrations with the same pattern override earlier ones. */
    public static void register(AbstractSpell spell) {
        if (BY_ID.containsKey(spell.id())) {
            throw new IllegalStateException("Duplicate spell id: " + spell.id());
        }
        BY_PATTERN.put(spell.pattern().key(), spell);
        BY_ID.put(spell.id(), spell);
    }

    /**
     * @return the spell for a rune sequence, or a shared Resonant Echo instance when
     *         nothing matches. Never returns null for a non-empty combination.
     */
    @Nullable
    public static AbstractSpell resolve(RuneCombination combination) {
        if (combination == null || combination.isEmpty()) {
            return null;
        }
        bootstrap();
        AbstractSpell exact = BY_PATTERN.get(combination.schoolKey());
        if (exact != null) {
            return exact;
        }
        String unordered = unorderedKey(combination);
        for (AbstractSpell spell : BY_PATTERN.values()) {
            if (unorderedKey(spell.pattern()).equals(unordered)) {
                return spell;
            }
        }
        return ResonantEchoSpell.INSTANCE;
    }

    private static String unorderedKey(SchoolPattern pattern) {
        List<String> ids = new ArrayList<>(pattern.schools().size());
        for (var school : pattern.schools()) {
            ids.add(school.id());
        }
        Collections.sort(ids);
        return String.join("+", ids);
    }

    private static String unorderedKey(RuneCombination combination) {
        List<String> ids = new ArrayList<>(combination.size());
        for (Rune rune : combination.runes()) {
            ids.add(rune.school().id());
        }
        Collections.sort(ids);
        return String.join("+", ids);
    }

    @Nullable
    public static AbstractSpell byId(Identifier id) {
        bootstrap();
        return BY_ID.get(id);
    }

    public static Collection<AbstractSpell> all() {
        bootstrap();
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    /** Spells a player can currently assemble from their unlocked runes. */
    public static List<AbstractSpell> knownTo(com.aetherius.api.magic.MagicUser magic) {
        List<AbstractSpell> out = new ArrayList<>();
        for (AbstractSpell spell : all()) {
            boolean known = true;
            for (var school : spell.pattern().schools()) {
                boolean hasSchool = false;
                for (Rune rune : magic.getUnlockedRunes()) {
                    if (rune.school() == school) {
                        hasSchool = true;
                        break;
                    }
                }
                if (!hasSchool) {
                    known = false;
                    break;
                }
            }
            if (known) {
                out.add(spell);
            }
        }
        return out;
    }

    /** Idempotent; safe to call from anywhere. */
    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        BaseSpells.registerAll();
        DuoSpells.registerAll();
        TrioSpells.registerAll();
    }
}
