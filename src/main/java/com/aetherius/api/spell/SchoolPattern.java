package com.aetherius.api.spell;

import com.aetherius.api.magic.SpellSchool;

import java.util.Arrays;
import java.util.List;

/**
 * An ordered school signature, e.g. {@code Caelum + Sol + Nox} for Nova Warp.
 *
 * <p>School order matters: {@code NOX+SOL+CAELUM} is a different spell than
 * {@code CAELUM+SOL+NOX}. This is the "sequence" in the Rune-Combination System:
 * which specific rune of a school you inscribed changes potency and cost, but the
 * <i>school order</i> decides which spell actually fires.</p>
 */
public record SchoolPattern(List<SpellSchool> schools) {

    public SchoolPattern {
        schools = List.copyOf(schools);
        if (schools.isEmpty() || schools.size() > 3) {
            throw new IllegalArgumentException("Spell patterns must contain 1-3 schools");
        }
    }

    public static SchoolPattern of(SpellSchool... schools) {
        return new SchoolPattern(Arrays.asList(schools));
    }

    /** Canonical lookup key, identical to {@link RuneCombination#schoolKey()}. */
    public String key() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < schools.size(); i++) {
            if (i > 0) {
                sb.append('+');
            }
            sb.append(schools.get(i).id());
        }
        return sb.toString();
    }

    public boolean matches(RuneCombination combination) {
        return key().equals(combination.schoolKey());
    }

    public static SchoolPattern parse(String key) {
        String[] parts = key.split("\\+");
        SpellSchool[] schools = new SpellSchool[parts.length];
        for (int i = 0; i < parts.length; i++) {
            schools[i] = SpellSchool.byId(parts[i]);
        }
        return new SchoolPattern(Arrays.asList(schools));
    }
}
