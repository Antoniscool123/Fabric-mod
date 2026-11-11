package com.aetherius.api.chronicle.objective;

import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.QuestObjective;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/** Objective satisfied by standing in a given biome; optionally requires the Monocle. */
public record BiomeObjective(String id, Identifier biomeId, boolean requireMonocle, String describeKey)
        implements QuestObjective {

    @Override
    public int target() {
        return 1;
    }

    @Override
    public int progress(ServerPlayerEntity player, PlayerChronicle chronicle) {
        if (requireMonocle) {
            var helmet = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof com.aetherius.item.AethersightMonocleItem)) {
                return 0;
            }
        }
        RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, biomeId);
        return player.getWorld().getBiome(player.getBlockPos()).matchesKey(key) ? 1 : 0;
    }
}
