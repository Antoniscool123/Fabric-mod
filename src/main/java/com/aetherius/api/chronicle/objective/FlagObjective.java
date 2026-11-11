package com.aetherius.api.chronicle.objective;

import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.QuestObjective;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Objective satisfied by an arbitrary story flag. Used for scripted beats that have no
 * natural inventory/entity signal: finishing a ritual, hearing a boss's second phase
 * dialogue, repairing the Observatory telescope.
 */
public record FlagObjective(String id, String flag, String describeKey) implements QuestObjective {

    @Override
    public int target() {
        return 1;
    }

    @Override
    public int progress(ServerPlayerEntity player, PlayerChronicle chronicle) {
        return chronicle.hasFlag(flag) ? 1 : 0;
    }
}
