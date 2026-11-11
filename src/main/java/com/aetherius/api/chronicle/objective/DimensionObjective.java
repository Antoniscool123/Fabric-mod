package com.aetherius.api.chronicle.objective;

import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.QuestObjective;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/** Objective satisfied by standing in a dimension (Act II Deep Dark / Nether trigger, Act III Sky). */
public record DimensionObjective(String id, RegistryKey<World> dimension, boolean requireMonocle,
                                 String describeKey) implements QuestObjective {

    @Override
    public int target() {
        return 1;
    }

    @Override
    public int progress(ServerPlayerEntity player, PlayerChronicle chronicle) {
        if (!player.getWorld().getRegistryKey().equals(dimension)) {
            return 0;
        }
        if (requireMonocle) {
            var helmet = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof com.aetherius.item.AethersightMonocleItem)) {
                return 0;
            }
        }
        return 1;
    }
}
