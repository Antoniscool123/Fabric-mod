package com.aetherius.registry;

import com.aetherius.item.AetherArmorMaterials;
import com.aetherius.item.AethersightMonocleItem;
import com.aetherius.item.AstrolabeItem;
import com.aetherius.item.ChronicleTomeItem;
import com.aetherius.item.StarlightElixirItem;
import com.aetherius.item.WandItem;
import com.aetherius.util.Aeth;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

/**
 * Item registry.
 *
 * <p>Ordering contract: {@link ModBlocks#register()} and {@link ModEntities#register()} run
 * first, then this class - block items and spawn eggs need those registry entries to exist.</p>
 */
public final class ModItems {

    // ---- Materials -------------------------------------------------------
    public static final Item AETHER_SHARD = register("aether_shard",
            new Item(new FabricItemSettings().rarity(Rarity.UNCOMMON)));
    public static final Item AETHER_BLOOD = register("aether_blood",
            new Item(new FabricItemSettings().rarity(Rarity.RARE)));
    public static final Item VOID_DUST = register("void_dust",
            new Item(new FabricItemSettings().rarity(Rarity.UNCOMMON)));
    public static final Item STAR_ASH_CLUMP = register("star_ash_clump",
            new Item(new FabricItemSettings()));

    // ---- Elemental Hearts (Act III keys) ---------------------------------
    public static final Item HEART_OF_SOL = register("heart_of_sol", heart(Rarity.RARE));
    public static final Item HEART_OF_LUNA = register("heart_of_luna", heart(Rarity.RARE));
    public static final Item HEART_OF_TERRA = register("heart_of_terra", heart(Rarity.RARE));
    public static final Item HEART_OF_CAELUM = register("heart_of_caelum", heart(Rarity.RARE));

    // ---- Tools & artifacts -----------------------------------------------
    public static final Item ASTRAL_WAND = register("astral_wand",
            new WandItem(WandItem.Tier.ASTRAL, new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item ECLIPSE_WAND = register("eclipse_wand",
            new WandItem(WandItem.Tier.ECLIPSE, new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Item ASTROLABE = register("astrolabe",
            new AstrolabeItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    public static final Item AETHERSIGHT_MONOCLE = register("aethersight_monocle",
            new AethersightMonocleItem(AetherArmorMaterials.AETHERSIGHT, ArmorItem.Type.HELMET,
                    new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    public static final Item CHRONICLE_TOME = register("chronicle_tome",
            new ChronicleTomeItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON)));

    // ---- Consumables ------------------------------------------------------
    public static final Item STARLIGHT_ELIXIR = register("starlight_elixir",
            new StarlightElixirItem(false, new FabricItemSettings().maxCount(16)));
    public static final Item PURIFIED_STARLIGHT_ELIXIR = register("purified_starlight_elixir",
            new StarlightElixirItem(true, new FabricItemSettings().maxCount(16).rarity(Rarity.UNCOMMON)));

    // ---- Block items ------------------------------------------------------
    public static final Item STAR_VEIN_ORE_ITEM = blockItem("star_vein_ore", ModBlocks.STAR_VEIN_ORE);
    public static final Item AETHER_BLOOD_ORE_ITEM = blockItem("aether_blood_ore", ModBlocks.AETHER_BLOOD_ORE);
    public static final Item STAR_ASH_ITEM = blockItem("star_ash", ModBlocks.STAR_ASH);
    public static final Item STARLIT_GRASS_BLOCK_ITEM =
            blockItem("starlit_grass_block", ModBlocks.STARLIT_GRASS_BLOCK);
    public static final Item MANA_LILY_ITEM = blockItem("mana_lily", ModBlocks.MANA_LILY);
    public static final Item AETHER_FOUNTAIN_ITEM = blockItem("aether_fountain", ModBlocks.AETHER_FOUNTAIN);
    public static final Item CELESTIAL_PILLAR_ITEM = blockItem("celestial_pillar", ModBlocks.CELESTIAL_PILLAR);
    public static final Item AETHER_FORGE_ITEM = blockItem("aether_forge", ModBlocks.AETHER_FORGE);
    public static final Item AETHER_ALTAR_CORE_ITEM = blockItem("aether_altar_core", ModBlocks.AETHER_ALTAR_CORE);
    public static final Item TELESCOPE_ITEM = blockItem("telescope", ModBlocks.TELESCOPE);
    public static final Item RUNE_LANTERN_ITEM = blockItem("rune_lantern", ModBlocks.RUNE_LANTERN);
    public static final Item SKY_RIFT_ANCHOR_ITEM = blockItem("sky_rift_anchor", ModBlocks.SKY_RIFT_ANCHOR);

    // ---- Spawn eggs -------------------------------------------------------
    public static final Item AETHER_MANTA_SPAWN_EGG = register("aether_manta_spawn_egg",
            new SpawnEggItem(ModEntities.AETHER_MANTA, 0x8FD6FF, 0xFFFFFF, new FabricItemSettings()));
    public static final Item VOID_SENTINEL_SPAWN_EGG = register("void_sentinel_spawn_egg",
            new SpawnEggItem(ModEntities.VOID_SENTINEL, 0x1B1030, 0x9B4DFF, new FabricItemSettings()));
    public static final Item VOID_SPECTER_SPAWN_EGG = register("void_specter_spawn_egg",
            new SpawnEggItem(ModEntities.VOID_SPECTER, 0x0A0614, 0xD9E7FF, new FabricItemSettings()));
    public static final Item ABOMINATION_SPAWN_EGG = register("void_warped_abomination_spawn_egg",
            new SpawnEggItem(ModEntities.VOID_WARPED_ABOMINATION, 0x3A2A1A, 0x9B4DFF, new FabricItemSettings()));
    public static final Item NOX_THAL_SPAWN_EGG = register("nox_thal_spawn_egg",
            new SpawnEggItem(ModEntities.NOX_THAL, 0x05030A, 0xFF8A3D, new FabricItemSettings()));

    private ModItems() {
    }

    private static Item heart(Rarity rarity) {
        return new Item(new FabricItemSettings().rarity(rarity).maxCount(4));
    }

    private static Item blockItem(String path, net.minecraft.block.Block block) {
        return register(path, new BlockItem(block, new FabricItemSettings()));
    }

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, Aeth.id(path), item);
    }

    public static void register() {
        // Class-loading registers every constant; method exists for call-site symmetry.
    }
}
