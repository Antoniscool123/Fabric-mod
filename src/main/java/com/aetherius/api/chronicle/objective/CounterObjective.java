package com.aetherius.api.chronicle.objective;

import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.QuestObjective;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Objective backed by a monotonic chronicle counter. Entity kills, block breaks and
 * spell casts all funnel through this so a single generic objective type covers them.
 */
public record CounterObjective(String id, String counter, int target, String describeKey)
        implements QuestObjective {

    @Override
    public int progress(ServerPlayerEntity player, PlayerChronicle chronicle) {
        return chronicle.counter(counter);
    }
}
