package com.aetherius.api.chronicle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-player Chronicle progression: completed quests, lore unlocks, counters, story flags
 * and the current Act.
 *
 * <p>Counters are the substrate the quest engine watches. Anything that happens in the
 * world - a kill, a block break, a cast - increments a named counter; objectives read
 * counters and live inventory/dimension state every second.</p>
 */
public final class PlayerChronicle {

    private final Set<Identifier> completed = new LinkedHashSet<>();
    private final Set<Identifier> unlockedLore = new LinkedHashSet<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<Identifier, Integer> completionOrder = new LinkedHashMap<>();
    private int act = 1;
    private boolean dirty = true;

    // ---- Quests ----------------------------------------------------------

    public boolean isComplete(Identifier questId) {
        return completed.contains(questId);
    }

    public void complete(Identifier questId) {
        if (completed.add(questId)) {
            completionOrder.put(questId, completed.size());
            dirty = true;
        }
    }

    public int completionIndex(Identifier questId) {
        return completionOrder.getOrDefault(questId, -1);
    }

    public Set<Identifier> completedQuests() {
        return Collections.unmodifiableSet(completed);
    }

    // ---- Counters --------------------------------------------------------

    public int counter(String key) {
        return counters.getOrDefault(key, 0);
    }

    public int increment(String key, int amount) {
        int next = counter(key) + amount;
        counters.put(key, next);
        dirty = true;
        return next;
    }

    public void setCounter(String key, int value) {
        counters.put(key, value);
        dirty = true;
    }

    public Map<String, Integer> counters() {
        return Collections.unmodifiableMap(counters);
    }

    // ---- Flags / milestones ---------------------------------------------

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public boolean setFlag(String flag) {
        boolean added = flags.add(flag);
        if (added) {
            dirty = true;
        }
        return added;
    }

    public Set<String> flags() {
        return Collections.unmodifiableSet(flags);
    }

    /** Alias used by spells and bosses that gate on story progress. */
    public boolean hasMilestone(String flag) {
        return hasFlag(flag);
    }

    // ---- Lore ------------------------------------------------------------

    public boolean unlockLore(Identifier loreId) {
        boolean added = unlockedLore.add(loreId);
        if (added) {
            dirty = true;
        }
        return added;
    }

    public boolean hasLore(Identifier loreId) {
        return unlockedLore.contains(loreId);
    }

    public Set<Identifier> unlockedLore() {
        return Collections.unmodifiableSet(unlockedLore);
    }

    // ---- Act -------------------------------------------------------------

    public int act() {
        return act;
    }

    /** Acts only ever move forward; returning to an earlier act is not a thing. */
    public void setAct(int act) {
        if (act > this.act) {
            this.act = Math.min(act, 3);
            dirty = true;
        }
    }

    // ---- Dirty tracking --------------------------------------------------

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    // ---- Persistence -----------------------------------------------------

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("Act", act);

        NbtList questList = new NbtList();
        for (Identifier id : completed) {
            questList.add(NbtString.of(id.toString()));
        }
        nbt.put("Completed", questList);

        NbtList loreList = new NbtList();
        for (Identifier id : unlockedLore) {
            loreList.add(NbtString.of(id.toString()));
        }
        nbt.put("Lore", loreList);

        NbtList flagList = new NbtList();
        for (String flag : new TreeSet<>(flags)) {
            flagList.add(NbtString.of(flag));
        }
        nbt.put("Flags", flagList);

        NbtCompound counterNbt = new NbtCompound();
        counters.forEach(counterNbt::putInt);
        nbt.put("Counters", counterNbt);
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return;
        }
        act = Math.max(1, nbt.getInt("Act"));

        completed.clear();
        readIdentifiers(nbt.getList("Completed", NbtElement.STRING_TYPE), completed);

        unlockedLore.clear();
        readIdentifiers(nbt.getList("Lore", NbtElement.STRING_TYPE), unlockedLore);

        flags.clear();
        NbtList flagList = nbt.getList("Flags", NbtElement.STRING_TYPE);
        for (int i = 0; i < flagList.size(); i++) {
            flags.add(flagList.getString(i));
        }

        counters.clear();
        NbtCompound counterNbt = nbt.getCompound("Counters");
        for (String key : counterNbt.getKeys()) {
            counters.put(key, counterNbt.getInt(key));
        }
        markDirty();
    }

    private static void readIdentifiers(NbtList list, Set<Identifier> target) {
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getString(i));
            if (id != null) {
                target.add(id);
            }
        }
    }

    public void copyFrom(PlayerChronicle other) {
        completed.clear();
        completed.addAll(other.completed);
        unlockedLore.clear();
        unlockedLore.addAll(other.unlockedLore);
        counters.clear();
        counters.putAll(other.counters);
        flags.clear();
        flags.addAll(other.flags);
        completionOrder.clear();
        completionOrder.putAll(other.completionOrder);
        act = other.act;
        markDirty();
    }
}
