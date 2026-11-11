package com.aetherius.registry;

import com.aetherius.util.Aeth;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

/** Single creative tab holding every Aetherius item, grouped by progression role. */
public final class ModItemGroups {

    public static final ItemGroup AETHERIUS = Registry.register(Registries.ITEM_GROUP,
            Aeth.id("aetherius"), FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.aetherius.aetherius"))
                    .icon(() -> new ItemStack(ModItems.AETHER_SHARD))
                    .entries((context, entries) -> {
                        entries.add(ModItems.AETHER_SHARD);
                        entries.add(ModItems.AETHER_BLOOD);
                        entries.add(ModItems.VOID_DUST);
                        entries.add(ModItems.STAR_ASH_CLUMP);
                        entries.add(ModItems.HEART_OF_SOL);
                        entries.add(ModItems.HEART_OF_LUNA);
                        entries.add(ModItems.HEART_OF_TERRA);
                        entries.add(ModItems.HEART_OF_CAELUM);
                        entries.add(ModItems.ASTRAL_WAND);
                        entries.add(ModItems.ECLIPSE_WAND);
                        entries.add(ModItems.ASTROLABE);
                        entries.add(ModItems.AETHERSIGHT_MONOCLE);
                        entries.add(ModItems.CHRONICLE_TOME);
                        entries.add(ModItems.STARLIGHT_ELIXIR);
                        entries.add(ModItems.PURIFIED_STARLIGHT_ELIXIR);
                        entries.add(ModItems.STAR_VEIN_ORE_ITEM);
                        entries.add(ModItems.AETHER_BLOOD_ORE_ITEM);
                        entries.add(ModItems.STAR_ASH_ITEM);
                        entries.add(ModItems.STARLIT_GRASS_BLOCK_ITEM);
                        entries.add(ModItems.MANA_LILY_ITEM);
                        entries.add(ModItems.AETHER_FOUNTAIN_ITEM);
                        entries.add(ModItems.CELESTIAL_PILLAR_ITEM);
                        entries.add(ModItems.AETHER_FORGE_ITEM);
                        entries.add(ModItems.AETHER_ALTAR_CORE_ITEM);
                        entries.add(ModItems.TELESCOPE_ITEM);
                        entries.add(ModItems.RUNE_LANTERN_ITEM);
                        entries.add(ModItems.SKY_RIFT_ANCHOR_ITEM);
                        entries.add(ModItems.AETHER_MANTA_SPAWN_EGG);
                        entries.add(ModItems.VOID_SENTINEL_SPAWN_EGG);
                        entries.add(ModItems.VOID_SPECTER_SPAWN_EGG);
                        entries.add(ModItems.ABOMINATION_SPAWN_EGG);
                        entries.add(ModItems.NOX_THAL_SPAWN_EGG);
                    })
                    .build());

    private ModItemGroups() {
    }

    public static void register() {
    }
}
