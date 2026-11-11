package com.aetherius.api.chronicle;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.api.spell.AbstractSpell;
import com.aetherius.api.spell.RuneCombination;
import com.aetherius.network.AetherNetworking;
import com.aetherius.registry.ModEntities;
import com.aetherius.registry.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Server-side Chronicle driver.
 *
 * <p>Two responsibilities, deliberately kept separate from content:</p>
 * <ol>
 *   <li><b>Event ingestion</b> - kill/break/cast/flag hooks translate gameplay into
 *       counters and flags on {@link PlayerChronicle}.</li>
 *   <li><b>Evaluation</b> - once a second, walk the quest table, complete anything whose
 *       objectives are all satisfied, grant rewards, advance the Act and push a sync.</li>
 * </ol>
 *
 * <p>Because objectives are pure queries, a quest can never "get stuck" on a missed
 * event: if the player already has the items or the flag, the next evaluation completes
 * it. This is what makes the system crash-safe and adds reload-safety.</p>
 */
public final class ChronicleManager {

    private static final ChronicleManager INSTANCE = new ChronicleManager();

    private int ticks;

    private ChronicleManager() {
        ChronicleRegistry.bootstrap();
    }

    public static ChronicleManager get() {
        return INSTANCE;
    }

    // =====================================================================
    //  Evaluation loop
    // =====================================================================

    public void onServerTick(MinecraftServer server) {
        if (++ticks % 20 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            try {
                evaluate(player);
            } catch (RuntimeException e) {
                com.aetherius.Aetherius.LOGGER.error("Chronicle evaluation failed for {}",
                        player.getGameProfile().getName(), e);
            }
        }
    }

    private void evaluate(ServerPlayerEntity player) {
        PlayerMagicCapability magic = MagicComponents.get(player);
        PlayerChronicle chronicle = magic.getChronicle();

        for (Quest quest : ChronicleRegistry.allQuests()) {
            if (chronicle.isComplete(quest.id()) || !quest.isUnlocked(chronicle)) {
                continue;
            }
            if (allObjectivesComplete(player, chronicle, quest)) {
                complete(player, magic, quest);
            }
        }

        advanceAct(chronicle);

        if (chronicle.isDirty() || magic.isDirty()) {
            AetherNetworking.syncMagic(player, magic);
        }
    }

    private boolean allObjectivesComplete(ServerPlayerEntity player, PlayerChronicle chronicle, Quest quest) {
        for (QuestObjective objective : quest.objectives()) {
            if (!objective.isComplete(player, chronicle)) {
                return false;
            }
        }
        return true;
    }

    private void complete(ServerPlayerEntity player, PlayerMagicCapability magic, Quest quest) {
        PlayerChronicle chronicle = magic.getChronicle();
        chronicle.complete(quest.id());

        if (!quest.milestoneFlag().isBlank()) {
            chronicle.setFlag(quest.milestoneFlag());
        }

        for (QuestReward reward : quest.rewards()) {
            try {
                reward.grant(player, magic);
            } catch (RuntimeException e) {
                com.aetherius.Aetherius.LOGGER.error("Reward failed for quest {}", quest.id(), e);
            }
        }

        player.sendMessage(Text.translatable("aetherius.chronicle.quest_complete",
                        Text.translatable(quest.titleKey()).formatted(Formatting.AQUA))
                .formatted(Formatting.GOLD), false);
        player.getWorld().playSound(null, player.getBlockPos(), ModSounds.CHRONICLE_UPDATE,
                SoundCategory.PLAYERS, 0.9f, 1.15f);
        magic.markDirty();
        chronicle.markDirty();
        AetherNetworking.syncMagic(player, magic);
        Audit.audit("quest", quest.id().getPath(), player);
    }

    /** Acts only advance; the highest fully-completed chapter sets the player's Act. */
    private void advanceAct(PlayerChronicle chronicle) {
        int targetAct = 1;
        for (Chapter chapter : List.of(Chapter.ACT_I, Chapter.ACT_II, Chapter.ACT_III)) {
            List<Quest> quests = ChronicleRegistry.questsFor(chapter);
            if (quests.isEmpty()) {
                continue;
            }
            boolean complete = true;
            for (Quest quest : quests) {
                if (!chronicle.isComplete(quest.id())) {
                    complete = false;
                    break;
                }
            }
            if (complete) {
                targetAct = Math.max(targetAct, chapter.act() + 1);
            }
        }
        chronicle.setAct(targetAct);
    }

    // =====================================================================
    //  Event ingestion
    // =====================================================================

    /** Called the instant a Star-Vein block is broken - the Act I inciting incident. */
    public void onStarVeinBroken(ServerPlayerEntity player) {
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        chronicle.increment(ChronicleCounters.STAR_VEIN_BROKEN, 1);
        chronicle.increment(ChronicleCounters.STARFALL_WITNESSED, 1);
        chronicle.markDirty();
    }

    /** Called when Aether-Blood ore yields its drop (the "divine blood" material). */
    public void onAetherBloodMined(ServerPlayerEntity player) {
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        chronicle.increment(ChronicleCounters.AETHER_BLOOD_MINED, 1);
        chronicle.markDirty();
    }

    public void onSpellCast(ServerPlayerEntity player, AbstractSpell spell, RuneCombination combination) {
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        if (spell.primarySchool() == SpellSchool.NOX || combination.distinctSchools().contains(SpellSchool.NOX)) {
            chronicle.increment(ChronicleCounters.NOX_CASTS, 1);
        }
        if (spell.id().getPath().equals("resonant_echo")) {
            chronicle.increment("resonant_echo_casts", 1);
        }
        chronicle.markDirty();
    }

    public void onEntityKilled(PlayerEntity attacker, LivingEntity victim) {
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        if (victim.getType() == ModEntities.VOID_SENTINEL) {
            chronicle.increment(ChronicleCounters.VOID_SENTINEL_KILLED, 1);
        } else if (victim.getType() == ModEntities.VOID_SPECTER) {
            chronicle.increment(ChronicleCounters.VOID_SPECTER_KILLED, 1);
        } else if (victim.getType() == ModEntities.VOID_WARPED_ABOMINATION) {
            chronicle.increment(ChronicleCounters.ABOMINATIONS_CLEANSED, 1);
        } else if (victim.getType() == ModEntities.EMBERS_WARDEN) {
            chronicle.increment(ChronicleCounters.WARDEN_SOL_KILLED, 1);
        } else if (victim.getType() == ModEntities.TIDES_WARDEN) {
            chronicle.increment(ChronicleCounters.WARDEN_LUNA_KILLED, 1);
        } else if (victim.getType() == ModEntities.STONES_WARDEN) {
            chronicle.increment(ChronicleCounters.WARDEN_TERRA_KILLED, 1);
        } else if (victim.getType() == ModEntities.STORMS_WARDEN) {
            chronicle.increment(ChronicleCounters.WARDEN_CAELUM_KILLED, 1);
        } else if (victim.getType() == ModEntities.GRAVITY_ANCHOR) {
            chronicle.increment(ChronicleCounters.GRAVITY_ANCHORS_DESTROYED, 1);
        }
        chronicle.markDirty();
    }

    public void onFlag(ServerPlayerEntity player, String flag) {
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        if (chronicle.setFlag(flag)) {
            chronicle.markDirty();
            AetherNetworking.syncMagic(player, MagicComponents.get(player));
        }
    }

    public void onFlag(ServerPlayerEntity player, String flag, String counter) {
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        chronicle.setFlag(flag);
        chronicle.increment(counter, 1);
        chronicle.markDirty();
        AetherNetworking.syncMagic(player, MagicComponents.get(player));
    }

    public void onBlightWitnessed(ServerPlayerEntity player) {
        onFlag(player, ChronicleCounters.FLAG_BLIGHT_WITNESSED,
                ChronicleCounters.BLIGHT_CHUNKS_WITNESSED);
    }

    public void onPurification(ServerPlayerEntity player) {
        onFlag(player, ChronicleCounters.FLAG_BLIGHT_PURIFIED,
                ChronicleCounters.ALTAR_PURIFICATIONS);
    }

    public void onObservatoryVisited(ServerPlayerEntity player) {
        PlayerChronicle chronicle = MagicComponents.get(player).getChronicle();
        chronicle.increment(ChronicleCounters.OBSERVATORY_VISITED, 1);
        chronicle.markDirty();
    }

    public void onObservatoryRepaired(ServerPlayerEntity player) {
        onFlag(player, ChronicleCounters.FLAG_OBSERVATORY_REPAIRED,
                ChronicleCounters.OBSERVATORY_REPAIRED);
    }

    public Quest quest(Identifier id) {
        return ChronicleRegistry.quest(id);
    }

    /** Tiny logging facade so this class does not import the mod entry point directly. */
    /** Small audit facade so quest mutations leave a server log trail. */
    private static final class Audit {
        static void audit(String type, String value, ServerPlayerEntity player) {
            com.aetherius.Aetherius.LOGGER.info("[Chronicle] {} {} -> {}", type, value,
                    player.getGameProfile().getName());
        }
    }
}
