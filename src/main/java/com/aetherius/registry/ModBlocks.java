package com.aetherius.registry;

import com.aetherius.block.AetherAltarBlock;
import com.aetherius.block.AetherForgeBlock;
import com.aetherius.block.AetherFountainBlock;
import com.aetherius.block.CelestialPillarBlock;
import com.aetherius.block.ManaLilyBlock;
import com.aetherius.block.RuneLanternBlock;
import com.aetherius.block.SkyRiftAnchorBlock;
import com.aetherius.block.SkyRiftPortalBlock;
import com.aetherius.block.StarAshBlock;
import com.aetherius.block.StarVeinOreBlock;
import com.aetherius.block.StarlitGrassBlock;
import com.aetherius.block.TelescopeBlock;
import com.aetherius.util.Aeth;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Block registry. Every block also has a {@code BlockItem} registered in {@link ModItems}
 * unless it is technical (portal). {@link #register()} must run before {@link ModItems}.
 */
public final class ModBlocks {

    // ---- Ores & terrain ---------------------------------------------------
    public static final Block STAR_VEIN_ORE = new StarVeinOreBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.PURPLE).requiresTool().strength(4.5f, 9.0f)
            .luminance(state -> 7).sounds(BlockSoundGroup.DEEPSLATE));

    public static final Block AETHER_BLOOD_ORE = new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.PALE_PURPLE).requiresTool().strength(3.5f, 6.0f)
            .luminance(state -> 3).sounds(BlockSoundGroup.STONE));

    public static final Block STAR_ASH = new StarAshBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GRAY).strength(0.6f).sounds(BlockSoundGroup.SOUL_SAND)
            .ticksRandomly());

    public static final Block STARLIT_GRASS_BLOCK = new StarlitGrassBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.LIGHT_BLUE).strength(0.7f).sounds(BlockSoundGroup.GRASS)
            .ticksRandomly());

    public static final Block MANA_LILY = new ManaLilyBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.CYAN).noCollision().breakInstantly().ticksRandomly()
            .sounds(BlockSoundGroup.GRASS).pistonBehavior(PistonBehavior.DESTROY));

    // ---- Magic infrastructure --------------------------------------------
    public static final Block AETHER_FOUNTAIN = new AetherFountainBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.DIAMOND_BLUE).strength(3.0f, 6.0f).luminance(state -> 12)
            .nonOpaque().sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block CELESTIAL_PILLAR = new CelestialPillarBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.TERRACOTTA_WHITE).requiresTool().strength(4.0f, 8.0f)
            .luminance(state -> 5).sounds(BlockSoundGroup.STONE));

    public static final Block AETHER_FORGE = new AetherForgeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.DIAMOND_BLUE).requiresTool().strength(5.0f, 12.0f)
            .luminance(state -> 10).sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block AETHER_ALTAR_CORE = new AetherAltarBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.WHITE).requiresTool().strength(6.0f, 20.0f)
            .luminance(state -> 13).sounds(BlockSoundGroup.AMETHYST_BLOCK));

    public static final Block TELESCOPE = new TelescopeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY).requiresTool().strength(3.5f, 8.0f)
            .luminance(state -> 4).sounds(BlockSoundGroup.COPPER));

    public static final Block RUNE_LANTERN = new RuneLanternBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GOLD).strength(1.0f).luminance(state -> 15)
            .sounds(BlockSoundGroup.LANTERN));

    // ---- Sky-Rift ---------------------------------------------------------
    public static final Block SKY_RIFT_ANCHOR = new SkyRiftAnchorBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK).requiresTool().strength(5.0f, 1200.0f)
            .luminance(state -> 9).sounds(BlockSoundGroup.DEEPSLATE));

    public static final Block SKY_RIFT_PORTAL = new SkyRiftPortalBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.PURPLE).noCollision().strength(-1.0f).luminance(state -> 11)
            .sounds(BlockSoundGroup.GLASS).pistonBehavior(PistonBehavior.BLOCK));

    private ModBlocks() {
    }

    private static Block register(String path, Block block) {
        return Registry.register(Registries.BLOCK, Aeth.id(path), block);
    }

    public static void register() {
        register("star_vein_ore", STAR_VEIN_ORE);
        register("aether_blood_ore", AETHER_BLOOD_ORE);
        register("star_ash", STAR_ASH);
        register("starlit_grass_block", STARLIT_GRASS_BLOCK);
        register("mana_lily", MANA_LILY);
        register("aether_fountain", AETHER_FOUNTAIN);
        register("celestial_pillar", CELESTIAL_PILLAR);
        register("aether_forge", AETHER_FORGE);
        register("aether_altar_core", AETHER_ALTAR_CORE);
        register("telescope", TELESCOPE);
        register("rune_lantern", RUNE_LANTERN);
        register("sky_rift_anchor", SKY_RIFT_ANCHOR);
        register("sky_rift_portal", SKY_RIFT_PORTAL);
    }

    /** Immutable-ish helper for the multiblock and worldgen code. */
    public static boolean isLapisOrQuartz(Block block) {
        return block == Blocks.LAPIS_BLOCK || block == Blocks.QUARTZ_BLOCK
                || block == Blocks.SMOOTH_QUARTZ || block == Blocks.CHISELED_QUARTZ_BLOCK;
    }
}
