package com.aetherius.api.chronicle;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;

/**
 * Immutable quest definition.
 *
 * @param id            globally unique quest id ({@code aetherius:act1_starfall})
 * @param chapter       which Act the quest is filed under in the Chronicle UI
 * @param order         sort order inside the chapter
 * @param titleKey      translation key for the title
 * @param loreKey       translation key for the body text shown in the right-hand pane
 * @param objectives    all must be complete to finish
 * @param rewards       granted once, on completion
 * @param requires      quest ids that must already be complete before this one appears
 * @param hidden        hidden until {@link #requires()} are met (mystery beats)
 * @param milestoneFlag story flag set on completion (drives {@code hasMilestone})
 */
public record Quest(Identifier id, Chapter chapter, int order, String titleKey, String loreKey,
                    List<QuestObjective> objectives, List<QuestReward> rewards,
                    Set<Identifier> requires, boolean hidden, String milestoneFlag) {

    public boolean isUnlocked(PlayerChronicle chronicle) {
        for (Identifier prerequisite : requires) {
            if (!chronicle.isComplete(prerequisite)) {
                return false;
            }
        }
        return true;
    }

    public String describeObjective(QuestObjective objective) {
        return objective.describeKey();
    }
}
