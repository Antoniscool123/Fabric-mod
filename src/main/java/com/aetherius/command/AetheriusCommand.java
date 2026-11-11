package com.aetherius.command;

import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.api.chronicle.ChronicleRegistry;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.entity.NoxThalEntity;
import com.aetherius.network.AetherNetworking;
import com.aetherius.registry.ModEntities;
import com.aetherius.world.AetheriusWorldState;
import com.aetherius.world.blight.CelestialBlightManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * {@code /aetherius ...} operator toolkit.
 *
 * <p>Everything a designer needs to exercise the systems without grinding: resource tuning,
 * rune unlocks, quest completion, blight inspection/purification and boss phase jumps.</p>
 */
public final class AetheriusCommand {

    private AetheriusCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("aetherius")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("mana")
                        .then(CommandManager.literal("fill").executes(ctx -> setMana(ctx, -1f)))
                        .then(CommandManager.literal("drain").executes(ctx -> setMana(ctx, 0f)))
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1000f))
                                .executes(ctx -> setMana(ctx, FloatArgumentType.getFloat(ctx, "value")))))
                .then(CommandManager.literal("tension")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 100f))
                                .executes(ctx -> setTension(ctx, FloatArgumentType.getFloat(ctx, "value")))))
                .then(CommandManager.literal("rune")
                        .then(CommandManager.literal("unlock_all").executes(AetheriusCommand::unlockAllRunes))
                        .then(CommandManager.literal("unlock")
                                .then(CommandManager.argument("rune", StringArgumentType.word())
                                        .executes(ctx -> unlockRune(ctx,
                                                StringArgumentType.getString(ctx, "rune")))))
                        .then(CommandManager.literal("list").executes(AetheriusCommand::listRunes)))
                .then(CommandManager.literal("spell")
                        .then(CommandManager.literal("list").executes(AetheriusCommand::listSpells)))
                .then(CommandManager.literal("quest")
                        .then(CommandManager.literal("list").executes(AetheriusCommand::listQuests))
                        .then(CommandManager.literal("complete")
                                .then(CommandManager.argument("id", StringArgumentType.string())
                                        .executes(ctx -> completeQuest(ctx,
                                                StringArgumentType.getString(ctx, "id"))))))
                .then(CommandManager.literal("blight")
                        .then(CommandManager.literal("info").executes(AetheriusCommand::blightInfo))
                        .then(CommandManager.literal("infect")
                                .then(CommandManager.argument("strength", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> infectBlight(ctx,
                                                IntegerArgumentType.getInteger(ctx, "strength")))))
                        .then(CommandManager.literal("purify")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 128))
                                        .executes(ctx -> purify(ctx,
                                                IntegerArgumentType.getInteger(ctx, "radius"))))))
                .then(CommandManager.literal("boss")
                        .then(CommandManager.literal("spawn").executes(AetheriusCommand::spawnBoss))
                        .then(CommandManager.literal("phase")
                                .then(CommandManager.argument("phase", IntegerArgumentType.integer(0, 2))
                                        .executes(ctx -> setBossPhase(ctx,
                                                IntegerArgumentType.getInteger(ctx, "phase"))))))
                .then(CommandManager.literal("chronicle").executes(AetheriusCommand::openChronicle))
                .then(CommandManager.literal("observatory").executes(AetheriusCommand::findObservatory))
                .then(CommandManager.literal("attune").executes(AetheriusCommand::attune)));
    }

    private static int setMana(CommandContext<ServerCommandSource> ctx, float value)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PlayerMagicCapability magic = MagicComponents.get(player);
        if (value < 0) {
            magic.setMana(magic.getMaxMana());
        } else {
            magic.setMana(Math.min(value, magic.getMaxMana()));
        }
        AetherNetworking.syncMagic(player, magic);
        ctx.getSource().sendFeedback(() -> Text.literal("Mana = " + magic.getMana()
                + " / " + magic.getMaxMana()).formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int setTension(CommandContext<ServerCommandSource> ctx, float value)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PlayerMagicCapability magic = MagicComponents.get(player);
        if (value >= 100f) {
            magic.triggerAstralBackfire();
        } else {
            magic.setAstralTension(value);
        }
        AetherNetworking.syncMagic(player, magic);
        ctx.getSource().sendFeedback(() -> Text.literal("Astral Tension = "
                + magic.getAstralTension()).formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int unlockAllRunes(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        PlayerMagicCapability magic = MagicComponents.get(player);
        for (Rune rune : Rune.values()) {
            magic.unlockRune(rune);
        }
        for (SpellSchool school : SpellSchool.values()) {
            magic.setSchoolMastery(school, 5);
        }
        AetherNetworking.syncMagic(player, magic);
        ctx.getSource().sendFeedback(() -> Text.literal("All " + Rune.values().length + " runes unlocked")
                .formatted(Formatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int unlockRune(CommandContext<ServerCommandSource> ctx, String name)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        Rune rune = Rune.byName(name);
        if (rune == null) {
            ctx.getSource().sendError(Text.literal("Unknown rune: " + name));
            return 0;
        }
        PlayerMagicCapability magic = MagicComponents.get(player);
        magic.unlockRune(rune);
        AetherNetworking.syncMagic(player, magic);
        ctx.getSource().sendFeedback(() -> Text.translatable("aetherius.command.rune_unlocked",
                Text.translatable(rune.translationKey())), false);
        return 1;
    }

    private static int listRunes(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        StringBuilder sb = new StringBuilder("Runes: ");
        for (Rune rune : MagicComponents.get(player).getUnlockedRunes()) {
            sb.append(rune.name()).append(' ');
        }
        String message = sb.toString();
        ctx.getSource().sendFeedback(() -> Text.literal(message).formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int listSpells(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("Spells (" + SpellRegistry.all().size() + "):")
                .formatted(Formatting.GOLD), false);
        SpellRegistry.all().forEach(spell -> ctx.getSource().sendFeedback(() ->
                Text.literal(" - " + spell.id() + "  [" + spell.pattern().key() + "]")
                        .formatted(Formatting.GRAY), false));
        return 1;
    }

    private static int listQuests(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var chronicle = MagicComponents.get(player).getChronicle();
        ChronicleRegistry.allQuests().forEach(quest -> ctx.getSource().sendFeedback(() ->
                Text.literal((chronicle.isComplete(quest.id()) ? "[x] " : "[ ] ") + quest.id())
                        .formatted(chronicle.isComplete(quest.id()) ? Formatting.GREEN : Formatting.GRAY), false));
        return 1;
    }

    private static int completeQuest(CommandContext<ServerCommandSource> ctx, String id)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        Identifier identifier = Identifier.tryParse(id);
        var quest = identifier == null ? null : ChronicleRegistry.quest(identifier);
        if (quest == null) {
            ctx.getSource().sendError(Text.literal("Unknown quest: " + id));
            return 0;
        }
        var chronicle = MagicComponents.get(player).getChronicle();
        chronicle.complete(quest.id());
        if (!quest.milestoneFlag().isBlank()) {
            chronicle.setFlag(quest.milestoneFlag());
        }
        for (var reward : quest.rewards()) {
            reward.grant(player, MagicComponents.get(player));
        }
        AetherNetworking.syncMagic(player, MagicComponents.get(player));
        ctx.getSource().sendFeedback(() -> Text.literal("Completed " + quest.id()), false);
        return 1;
    }

    private static int blightInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        AetheriusWorldState state = AetheriusWorldState.get(player.getServerWorld());
        int intensity = CelestialBlightManager.get().intensity(player.getServerWorld(), player.getBlockPos());
        ctx.getSource().sendFeedback(() -> Text.literal("Chunk intensity: " + intensity
                + " | infected chunks: " + state.blightSnapshot().size()
                + " | observatories: " + state.observatories().size()), false);
        return 1;
    }

    private static int infectBlight(CommandContext<ServerCommandSource> ctx, int strength)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        CelestialBlightManager.get().infect(player.getServerWorld(), player.getBlockPos(), strength);
        ctx.getSource().sendFeedback(() -> Text.literal("Blight seeded (" + strength + ")")
                .formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int purify(CommandContext<ServerCommandSource> ctx, int radius)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        int cleared = CelestialBlightManager.get().purify(player.getServerWorld(), player.getBlockPos(), radius);
        ctx.getSource().sendFeedback(() -> Text.literal("Purified " + cleared + " chunk(s)")
                .formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int spawnBoss(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        NoxThalEntity boss = ModEntities.NOX_THAL.create(player.getServerWorld());
        if (boss == null) {
            return 0;
        }
        BlockPos spawn = player.getBlockPos().add(0, 12, 18);
        boss.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f);
        boss.setTarget(player);
        player.getServerWorld().spawnEntity(boss);
        ctx.getSource().sendFeedback(() -> Text.literal("Nox'Thal has entered the sky")
                .formatted(Formatting.DARK_PURPLE), false);
        return 1;
    }

    private static int setBossPhase(CommandContext<ServerCommandSource> ctx, int phase)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var bosses = player.getServerWorld().getEntitiesByClass(NoxThalEntity.class,
                player.getBoundingBox().expand(96.0), e -> true);
        if (bosses.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No Nox'Thal nearby"));
            return 0;
        }
        NoxThalEntity boss = bosses.get(0);
        boss.setPhase(phase);
        ctx.getSource().sendFeedback(() -> Text.literal("Nox'Thal -> phase " + (phase + 1)), false);
        return 1;
    }

    private static int openChronicle(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        AetherNetworking.openChronicle(ctx.getSource().getPlayerOrThrow());
        return 1;
    }

    private static int findObservatory(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var nearest = AetheriusWorldState.get(player.getServerWorld())
                .findNearestObservatory(player.getBlockPos());
        if (nearest.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No observatories recorded in generated chunks"));
            return 0;
        }
        BlockPos pos = nearest.get();
        ctx.getSource().sendFeedback(() -> Text.literal("Nearest observatory: " + pos.getX() + ", "
                + pos.getY() + ", " + pos.getZ()), false);
        return 1;
    }

    private static int attune(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ChronicleManager.get().onFlag(ctx.getSource().getPlayerOrThrow(),
                com.aetherius.api.chronicle.ChronicleCounters.FLAG_ACT2_ATTUNED);
        ctx.getSource().sendFeedback(() -> Text.literal("Act II attunement granted"), false);
        return 1;
    }
}
