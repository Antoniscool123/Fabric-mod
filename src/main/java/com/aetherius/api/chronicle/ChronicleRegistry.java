package com.aetherius.api.chronicle;

import com.aetherius.api.chronicle.objective.AnyOfObjective;
import com.aetherius.api.chronicle.objective.CounterObjective;
import com.aetherius.api.chronicle.objective.DimensionObjective;
import com.aetherius.api.chronicle.objective.FlagObjective;
import com.aetherius.api.chronicle.objective.ItemObjective;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.dimension.AetherDimensions;
import com.aetherius.registry.ModItems;
import com.aetherius.util.Aeth;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The complete Chronicle content table: 15 quests across three Acts plus the lore
 * appendix. Text lives in {@code lang/en_us.json} so it can be translated and so the
 * narrative can be rewritten without touching Java.
 *
 * <h2>Progression spine</h2>
 * <pre>
 * ACT I   star-vein break -> starfall -> aether-blood -> Astrolabe -> Observatory repair
 * ACT II  monocle + Deep Dark/Nether -> blight witnessed -> Altar built -> purification rite
 * ACT III four Elemental Hearts -> Sky-Rift -> Shattered Sky -> anchors -> Nox'Thal
 * </pre>
 */
public final class ChronicleRegistry {

    private static final Map<Identifier, Quest> QUESTS = new LinkedHashMap<>();
    private static final Map<Identifier, LoreEntry> LORE = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ChronicleRegistry() {
    }

    /** A lore page unlocked by quests, structures or boss phases. */
    public record LoreEntry(Identifier id, Chapter chapter, String titleKey, String bodyKey) {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        defineActOne();
        defineActTwo();
        defineActThree();
        defineLore();
    }

    // =====================================================================
    //  ACT I - The Fallen Starlight
    // =====================================================================

    private static void defineActOne() {
        quest("act1_starfall", Chapter.ACT_I, 0,
                List.of(new CounterObjective("break_vein", ChronicleCounters.STAR_VEIN_BROKEN, 1,
                                "aetherius.objective.break_star_vein"),
                        new ItemObjective("shard", ModItems.AETHER_SHARD, 2,
                                "aetherius.objective.collect_aether_shard")),
                List.of(QuestReward.ofItem(ModItems.CHRONICLE_TOME.getDefaultStack()),
                        QuestReward.ofMana(10f),
                        QuestReward.ofLore(Aeth.id("lore.first_convergence"))),
                Set.of(), false, "");

        quest("act1_blood", Chapter.ACT_I, 1,
                List.of(new ItemObjective("blood", ModItems.AETHER_BLOOD, 4,
                        "aetherius.objective.collect_aether_blood")),
                List.of(QuestReward.ofRune(Rune.FLARIS),
                        QuestReward.ofMastery(SpellSchool.SOL, 1)),
                Set.of(Aeth.id("act1_starfall")), false, "");

        quest("act1_astrolabe", Chapter.ACT_I, 2,
                List.of(new ItemObjective("astrolabe", ModItems.ASTROLABE, 1,
                        "aetherius.objective.craft_astrolabe")),
                List.of(QuestReward.ofRune(Rune.GELU),
                        QuestReward.ofLore(Aeth.id("lore.sky_temples"))),
                Set.of(Aeth.id("act1_blood")), false, "act1_astrolabe");

        quest("act1_observatory", Chapter.ACT_I, 3,
                List.of(new CounterObjective("visit", ChronicleCounters.OBSERVATORY_VISITED, 1,
                                "aetherius.objective.find_observatory"),
                        new FlagObjective("repair", ChronicleCounters.FLAG_OBSERVATORY_REPAIRED,
                                "aetherius.objective.repair_telescope")),
                List.of(QuestReward.ofRune(Rune.LUNARIS),
                        QuestReward.ofMastery(SpellSchool.CAELUM, 1),
                        QuestReward.ofLore(Aeth.id("lore.telescope"))),
                Set.of(Aeth.id("act1_astrolabe")), false, "");

        quest("act1_herald", Chapter.ACT_I, 4,
                List.of(new CounterObjective("sentinel", ChronicleCounters.VOID_SENTINEL_KILLED, 1,
                        "aetherius.objective.slay_void_sentinel")),
                List.of(QuestReward.ofRune(Rune.MURUS),
                        QuestReward.ofExperience(3),
                        QuestReward.ofLore(Aeth.id("lore.void_sentinels"))),
                Set.of(Aeth.id("act1_observatory")), false, ChronicleCounters.FLAG_ACT1_COMPLETE);
    }

    // =====================================================================
    //  ACT II - The Weave & The Rot
    // =====================================================================

    private static void defineActTwo() {
        quest("act2_sight", Chapter.ACT_II, 0,
                List.of(new AnyOfObjective("deep_dark_or_nether",
                        List.of(new com.aetherius.api.chronicle.objective.BiomeObjective("deep_dark",
                                        new net.minecraft.util.Identifier("minecraft", "deep_dark"), true,
                                        "aetherius.objective.wear_monocle_deep_dark"),
                                new DimensionObjective("nether", net.minecraft.world.World.NETHER, true,
                                        "aetherius.objective.wear_monocle_nether")),
                        "aetherius.objective.wear_monocle_underground")),
                List.of(QuestReward.ofRune(Rune.HIATUS),
                        QuestReward.ofLore(Aeth.id("lore.aethersight"))),
                Set.of(Aeth.id("act1_herald")), false, ChronicleCounters.FLAG_ACT2_ATTUNED);

        quest("act2_rot", Chapter.ACT_II, 1,
                List.of(new CounterObjective("witness", ChronicleCounters.BLIGHT_CHUNKS_WITNESSED, 1,
                                "aetherius.objective.witness_blight"),
                        new CounterObjective("nox", ChronicleCounters.NOX_CASTS, 10,
                                "aetherius.objective.cast_void")),
                List.of(QuestReward.ofRune(Rune.TERRANOX),
                        QuestReward.ofLore(Aeth.id("lore.celestial_blight"))),
                Set.of(Aeth.id("act2_sight")), false, ChronicleCounters.FLAG_BLIGHT_WITNESSED);

        quest("act2_altar", Chapter.ACT_II, 2,
                List.of(new FlagObjective("altar", ChronicleCounters.FLAG_ALTAR_BUILT,
                        "aetherius.objective.build_altar")),
                List.of(QuestReward.ofMastery(SpellSchool.TERRA, 1),
                        QuestReward.ofItem(Items.AMETHYST_SHARD.getDefaultStack()),
                        QuestReward.ofLore(Aeth.id("lore.purification"))),
                Set.of(Aeth.id("act2_rot")), false, "");

        quest("act2_purify", Chapter.ACT_II, 3,
                List.of(new CounterObjective("rites", ChronicleCounters.ALTAR_PURIFICATIONS, 1,
                        "aetherius.objective.perform_rite")),
                List.of(QuestReward.ofRune(Rune.SOLARIS),
                        QuestReward.ofMana(15f),
                        QuestReward.ofLore(Aeth.id("lore.first_convergence")),
                        QuestReward.ofTensionResistance(0.25f)),
                Set.of(Aeth.id("act2_altar")), false, ChronicleCounters.FLAG_BLIGHT_PURIFIED);
    }

    // =====================================================================
    //  ACT III - The Shattered Horizon
    // =====================================================================

    private static void defineActThree() {
        quest("act3_hearts", Chapter.ACT_III, 0,
                List.of(new ItemObjective("sol", ModItems.HEART_OF_SOL, 1, "aetherius.objective.heart_sol"),
                        new ItemObjective("luna", ModItems.HEART_OF_LUNA, 1, "aetherius.objective.heart_luna"),
                        new ItemObjective("terra", ModItems.HEART_OF_TERRA, 1, "aetherius.objective.heart_terra"),
                        new ItemObjective("caelum", ModItems.HEART_OF_CAELUM, 1,
                                "aetherius.objective.heart_caelum")),
                List.of(QuestReward.ofRune(Rune.ECLIPSE),
                        QuestReward.ofLore(Aeth.id("lore.elemental_hearts"))),
                Set.of(Aeth.id("act2_purify")), false, "");

        quest("act3_portal", Chapter.ACT_III, 1,
                List.of(new FlagObjective("rift", ChronicleCounters.FLAG_SKY_RIFT_OPEN,
                        "aetherius.objective.open_sky_rift")),
                List.of(QuestReward.ofMana(20f),
                        QuestReward.ofLore(Aeth.id("lore.shattered_sky"))),
                Set.of(Aeth.id("act3_hearts")), false, "");

        quest("act3_arrival", Chapter.ACT_III, 2,
                List.of(new DimensionObjective("sky", AetherDimensions.SHATTERED_SKY, false,
                        "aetherius.objective.enter_shattered_sky")),
                List.of(QuestReward.ofRune(Rune.CAELUS),
                        QuestReward.ofLore(Aeth.id("lore.aether_mantas"))),
                Set.of(Aeth.id("act3_portal")), false, ChronicleCounters.FLAG_SHATTERED_SKY_REACHED);

        quest("act3_anchors", Chapter.ACT_III, 3,
                List.of(new CounterObjective("anchors", ChronicleCounters.GRAVITY_ANCHORS_DESTROYED, 8,
                        "aetherius.objective.destroy_anchors")),
                List.of(QuestReward.ofMastery(SpellSchool.CAELUM, 1),
                        QuestReward.ofLore(Aeth.id("lore.nox_thal"))),
                Set.of(Aeth.id("act3_arrival")), false, ChronicleCounters.FLAG_ANCHOR_GROUNDED);

        quest("act3_nox", Chapter.ACT_III, 4,
                List.of(new FlagObjective("kill", ChronicleCounters.FLAG_NOX_THAL_SLAIN,
                        "aetherius.objective.slay_nox_thal")),
                List.of(QuestReward.ofLore(Aeth.id("lore.epilogue")),
                        QuestReward.ofExperience(30),
                        QuestReward.ofMana(30f)),
                Set.of(Aeth.id("act3_anchors")), true, "");
    }

    // =====================================================================
    //  Lore appendix
    // =====================================================================

    private static void defineLore() {
        lore("first_convergence", Chapter.ACT_I);
        lore("sky_temples", Chapter.ACT_I);
        lore("telescope", Chapter.ACT_I);
        lore("void_sentinels", Chapter.ACT_I);
        lore("aethersight", Chapter.ACT_II);
        lore("celestial_blight", Chapter.ACT_II);
        lore("purification", Chapter.ACT_II);
        lore("elemental_hearts", Chapter.ACT_III);
        lore("shattered_sky", Chapter.ACT_III);
        lore("aether_mantas", Chapter.ACT_III);
        lore("nox_thal", Chapter.ACT_III);
        lore("epilogue", Chapter.APPENDIX);
    }

    // =====================================================================
    //  Builders / lookups
    // =====================================================================

    private static void quest(String path, Chapter chapter, int order, List<QuestObjective> objectives,
                              List<QuestReward> rewards, Set<Identifier> requires, boolean hidden,
                              String milestoneFlag) {
        Identifier id = Aeth.id(path);
        QUESTS.put(id, new Quest(id, chapter, order,
                "aetherius.quest." + path + ".title",
                "aetherius.quest." + path + ".lore",
                objectives, rewards, requires, hidden, milestoneFlag));
    }

    private static void lore(String path, Chapter chapter) {
        Identifier id = Aeth.id("lore." + path);
        LORE.put(id, new LoreEntry(id, chapter,
                "aetherius.lore." + path + ".title",
                "aetherius.lore." + path + ".body"));
    }

    public static Quest quest(Identifier id) {
        bootstrap();
        return QUESTS.get(id);
    }

    public static List<Quest> allQuests() {
        bootstrap();
        return List.copyOf(QUESTS.values());
    }

    public static List<Quest> questsFor(Chapter chapter) {
        List<Quest> out = new ArrayList<>();
        for (Quest quest : allQuests()) {
            if (quest.chapter() == chapter) {
                out.add(quest);
            }
        }
        out.sort((a, b) -> Integer.compare(a.order(), b.order()));
        return Collections.unmodifiableList(out);
    }

    public static LoreEntry lore(Identifier id) {
        bootstrap();
        return LORE.get(id);
    }

    public static List<LoreEntry> loreFor(Chapter chapter) {
        bootstrap();
        List<LoreEntry> out = new ArrayList<>();
        for (LoreEntry entry : LORE.values()) {
            if (entry.chapter() == chapter) {
                out.add(entry);
            }
        }
        return out;
    }

    public static List<LoreEntry> allLore() {
        bootstrap();
        return List.copyOf(LORE.values());
    }
}
