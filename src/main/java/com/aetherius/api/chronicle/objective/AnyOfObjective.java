package com.aetherius.api.chronicle.objective;

import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.QuestObjective;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Composite objective: satisfied when <i>any</i> child objective is satisfied.
 * Used for "Deep Dark <b>or</b> Nether" style Act triggers where several world
 * states are equally valid ways to advance the story.
 */
public record AnyOfObjective(String id, List<QuestObjective> alternatives, String describeKey)
        implements QuestObjective {

    @Override
    public int target() {
        return 1;
    }

    @Override
    public int progress(ServerPlayerEntity player, PlayerChronicle chronicle) {
        for (QuestObjective objective : alternatives) {
            if (objective.isComplete(player, chronicle)) {
                return 1;
            }
        }
        return 0;
    }
}
