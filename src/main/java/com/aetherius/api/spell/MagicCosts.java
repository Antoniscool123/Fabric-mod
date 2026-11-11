package com.aetherius.api.spell;

import com.aetherius.api.magic.Rune;
import net.minecraft.util.math.MathHelper;

/**
 * Cost table for a rune sequence.
 *
 * <p>Costs scale super-linearly with repeated schools, which is what makes
 * {@code Sol+Sol+Sol} a bad idea compared to {@code Sol+Caelum+Nox}: the second and
 * third rune of the same school cost more than the first.</p>
 */
public record MagicCosts(float mana, float tension) {

    /** Builds the cost for an ordered rune sequence. */
    public static MagicCosts of(RuneCombination combination) {
        float mana = 0f;
        float tension = 0f;
        int sameSchoolStreak = 0;
        Rune previous = null;
        for (Rune rune : combination.runes()) {
            mana += rune.manaCost();
            tension += rune.tensionCost();
            if (rune.school() == previous) {
                sameSchoolStreak++;
                mana += rune.manaCost() * 0.25f * sameSchoolStreak;
                tension += rune.tensionCost() * 0.15f * sameSchoolStreak;
            } else {
                sameSchoolStreak = 0;
            }
            previous = rune;
        }
        // Three distinct schools earns a "Concordance" discount (combinatorics reward).
        if (combination.distinctSchools() == 3) {
            mana *= 0.9f;
        }
        return new MagicCosts(MathHelper.ceil(mana), tension);
    }
}
