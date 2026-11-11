package com.aetherius.api.chronicle.objective;

import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.QuestObjective;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Objective counting an item across the player's whole inventory (live query, so an
 * item that is dropped re-locks the objective until it is picked up again - which is
 * exactly what we want for "carry the Astrolabe to the temple" beats).
 */
public record ItemObjective(String id, Item item, int target, String describeKey) implements QuestObjective {

    @Override
    public int progress(ServerPlayerEntity player, PlayerChronicle chronicle) {
        int count = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            var stack = inventory.getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return Math.min(count, target);
    }
}
