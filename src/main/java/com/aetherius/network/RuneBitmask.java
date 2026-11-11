package com.aetherius.network;

import com.aetherius.api.magic.Rune;

import java.util.EnumSet;
import java.util.Set;

/**
 * Packs the set of unlocked runes into a single short. The rune enum is designed to stay
 * under 16 entries; adding runes past that requires widening to an int here and in the
 * sync packet (kept explicit on purpose so it cannot silently corrupt).
 */
public final class RuneBitmask {
    private RuneBitmask() {
    }

    public static short toMask(Set<Rune> runes) {
        int mask = 0;
        for (Rune rune : runes) {
            mask |= 1 << rune.ordinal();
        }
        return (short) mask;
    }

    public static Set<Rune> fromMask(short mask) {
        Set<Rune> out = EnumSet.noneOf(Rune.class);
        for (Rune rune : Rune.values()) {
            if ((mask & (1 << rune.ordinal())) != 0) {
                out.add(rune);
            }
        }
        return out;
    }
}
