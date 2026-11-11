package com.aetherius.api.spell;

import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * An ordered inscription of 1-3 runes, e.g. {@code [VENTUS, IGNIS, UMBRA]}
 * which is {@code Caelum + Sol + Nox} -> Nova Warp.
 *
 * <p>Serialised onto wand item NBT under the {@code Runes} list, so wands are
 * data-driven and survive anvils/creative copying.</p>
 */
public final class RuneCombination {

    public static final int MAX_RUNES = 3;
    public static final RuneCombination EMPTY = new RuneCombination(List.of());

    private final List<Rune> runes;
    private final String schoolKey;

    private RuneCombination(List<Rune> runes) {
        this.runes = List.copyOf(runes);
        this.schoolKey = buildSchoolKey(this.runes);
    }

    public static RuneCombination of(Rune... runes) {
        return new RuneCombination(Arrays.asList(runes));
    }

    public static RuneCombination of(List<Rune> runes) {
        return new RuneCombination(runes);
    }

    private static String buildSchoolKey(List<Rune> runes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runes.size(); i++) {
            if (i > 0) {
                sb.append('+');
            }
            sb.append(runes.get(i).school().id());
        }
        return sb.toString();
    }

    public List<Rune> runes() {
        return runes;
    }

    public int size() {
        return runes.size();
    }

    public boolean isEmpty() {
        return runes.isEmpty();
    }

    public String schoolKey() {
        return schoolKey;
    }

    public SchoolPattern pattern() {
        List<SpellSchool> schools = new ArrayList<>(runes.size());
        for (Rune rune : runes) {
            schools.add(rune.school());
        }
        return new SchoolPattern(schools);
    }

    public Set<SpellSchool> distinctSchools() {
        Set<SpellSchool> set = EnumSet.noneOf(SpellSchool.class);
        for (Rune rune : runes) {
            set.add(rune.school());
        }
        return set;
    }

    /** 1.0 for all-tier-I, up to ~1.6 for all-tier-III. Drives spell potency. */
    public float averageTier() {
        if (runes.isEmpty()) {
            return 1f;
        }
        int total = 0;
        for (Rune rune : runes) {
            total += rune.tier();
        }
        return (float) total / runes.size();
    }

    public RuneCombination append(Rune rune) {
        List<Rune> next = new ArrayList<>(runes);
        if (next.size() >= MAX_RUNES) {
            next.remove(0);
        }
        next.add(rune);
        return new RuneCombination(next);
    }

    public RuneCombination clear() {
        return EMPTY;
    }

    // ---- NBT -------------------------------------------------------------

    public void writeNbt(NbtCompound nbt, String key) {
        NbtList list = new NbtList();
        for (Rune rune : runes) {
            list.add(NbtString.of(rune.name()));
        }
        nbt.put(key, list);
    }

    public static RuneCombination readNbt(NbtCompound nbt, String key) {
        if (!nbt.contains(key, NbtElement.LIST_TYPE)) {
            return EMPTY;
        }
        List<Rune> parsed = new ArrayList<>(MAX_RUNES);
        NbtList list = nbt.getList(key, NbtElement.STRING_TYPE);
        for (int i = 0; i < Math.min(list.size(), MAX_RUNES); i++) {
            Rune rune = Rune.byName(list.getString(i));
            if (rune != null) {
                parsed.add(rune);
            }
        }
        return parsed.isEmpty() ? EMPTY : new RuneCombination(parsed);
    }

    public String displayString() {
        if (runes.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runes.size(); i++) {
            if (i > 0) {
                sb.append(" > ");
            }
            sb.append(runes.get(i).name());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "RuneCombination[" + schoolKey + "]";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RuneCombination combo && combo.runes.equals(this.runes);
    }

    @Override
    public int hashCode() {
        return runes.hashCode();
    }

    public static List<RuneCombination> allOfSize(int size, java.util.Collection<Rune> pool) {
        List<RuneCombination> out = new ArrayList<>();
        List<Rune> candidates = new ArrayList<>(pool);
        for (Rune a : candidates) {
            if (size == 1) {
                out.add(of(a));
                continue;
            }
            for (Rune b : candidates) {
                if (size == 2) {
                    out.add(of(a, b));
                    continue;
                }
                for (Rune c : candidates) {
                    out.add(of(a, b, c));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }
}
