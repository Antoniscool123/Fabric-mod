package com.aetherius.api.chronicle;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A single, self-evaluating quest requirement.
 *
 * <p>Objectives are pure: given the player and their chronicle they return the current
 * progress value. The manager compares it to {@link #target()} every second, so
 * objectives never need to subscribe to events themselves - events only bump counters
 * and set flags (see {@link PlayerChronicle}).</p>
 */
public interface QuestObjective {

    /** Unique within its quest; also used as the synced tooltip key suffix. */
    String id();

    String describeKey();

    int target();

    int progress(ServerPlayerEntity player, PlayerChronicle chronicle);

    default boolean isComplete(ServerPlayerEntity player, PlayerChronicle chronicle) {
        return progress(player, chronicle) >= target();
    }

    default String counterKey() {
        return id();
    }
}
