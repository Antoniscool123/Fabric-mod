package com.aetherius.network;

import com.aetherius.Aetherius;
import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.api.spell.RuneCombination;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.item.WandItem;
import com.aetherius.util.Aeth;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * All Aetherius packet ids plus the server-side receivers.
 *
 * <p>Design rules enforced here:</p>
 * <ul>
 *   <li>Receivers never trust the client: every cast is re-validated against the
 *       server's capability (mana, tension, known runes, cooldowns).</li>
 *   <li>Byte buffers are read inside the network thread, then the game logic is
 *       scheduled with {@code server.execute(...)}.</li>
 *   <li>Client-bound packets are small and idempotent.</li>
 * </ul>
 */
public final class AetherNetworking {

    // Client -> Server
    public static final Identifier CAST_SPELL = Aeth.id("cast_spell");
    public static final Identifier INSCRIBE_RUNE = Aeth.id("inscribe_rune");
    public static final Identifier CHRONICLE_ACTION = Aeth.id("chronicle_action");

    // Server -> Client
    public static final Identifier MAGIC_SYNC = Aeth.id("magic_sync");
    public static final Identifier CAST_FEEDBACK = Aeth.id("cast_feedback");
    public static final Identifier CHRONICLE_SYNC = Aeth.id("chronicle_sync");
    public static final Identifier SCREEN_SHAKE = Aeth.id("screen_shake");
    public static final Identifier BOSS_EVENT = Aeth.id("boss_event");
    public static final Identifier OPEN_CHRONICLE = Aeth.id("open_chronicle");

    private AetherNetworking() {
    }

    // =====================================================================
    //  Server -> Client senders
    // =====================================================================

    public static void syncMagic(ServerPlayerEntity player, PlayerMagicCapability magic) {
        MagicSyncS2CPacket.send(player, magic);
        ChronicleSyncS2CPacket.send(player, magic);
        magic.clearDirty();
        magic.getChronicle().clearDirty();
    }

    public static void syncChronicle(ServerPlayerEntity player, PlayerMagicCapability magic) {
        ChronicleSyncS2CPacket.send(player, magic);
    }

    public static void sendCastFeedback(ServerPlayerEntity player, boolean success,
                                        Identifier spellId, String reasonKey) {
        CastFeedbackS2CPacket.send(player, success, spellId, reasonKey);
    }

    public static void sendScreenShake(ServerWorld world, net.minecraft.util.math.BlockPos origin,
                                       double radius, int intensity) {
        ScreenShakeS2CPacket.send(world, origin, radius, intensity);
    }

    /** Opens the book and immediately pushes a snapshot, so the client never renders stale data. */
    public static void openChronicle(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN_CHRONICLE, PacketByteBufs.empty());
        ChronicleSyncS2CPacket.send(player, MagicComponents.get(player));
    }

    // =====================================================================
    //  Server-side receivers (registered from the mod initialiser)
    // =====================================================================

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CAST_SPELL,
                (server, player, handler, buf, responseSender) -> {
                    int handOrdinal = buf.readByte();
                    int size = buf.readByte();
                    List<Rune> runes = new ArrayList<>(3);
                    for (int i = 0; i < size; i++) {
                        int ordinal = buf.readByte();
                        if (ordinal >= 0 && ordinal < Rune.values().length) {
                            runes.add(Rune.values()[ordinal]);
                        }
                    }
                    Hand hand = handOrdinal == 1 ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    server.execute(() -> handleCast(player, hand, RuneCombination.of(runes)));
                });

        ServerPlayNetworking.registerGlobalReceiver(INSCRIBE_RUNE,
                (server, player, handler, buf, responseSender) -> {
                    int action = buf.readByte();
                    int runeOrdinal = buf.readByte();
                    server.execute(() -> handleInscribe(player, action, runeOrdinal));
                });

        ServerPlayNetworking.registerGlobalReceiver(CHRONICLE_ACTION,
                (server, player, handler, buf, responseSender) -> {
                    int action = buf.readByte();
                    String questId = buf.readString(128);
                    server.execute(() -> handleChronicleAction(player, action, questId));
                });
    }

    private static void handleCast(ServerPlayerEntity player, Hand hand, RuneCombination combination) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof WandItem wand)) {
            sendCastFeedback(player, false, null, "aetherius.cast.no_wand");
            return;
        }
        PlayerMagicCapability magic = MagicComponents.get(player);
        var result = magic.cast(combination);
        if (!result.success()) {
            sendCastFeedback(player, false, null, result.messageKey());
            return;
        }
        wand.onSuccessfulCast(player, stack, result, combination);
        sendCastFeedback(player, true, result.spellId(), result.messageKey());
        syncMagic(player, magic);
    }

    private static void handleInscribe(ServerPlayerEntity player, int action, int runeOrdinal) {
        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
        if (!(stack.getItem() instanceof WandItem)) {
            stack = player.getStackInHand(Hand.OFF_HAND);
        }
        if (!(stack.getItem() instanceof WandItem wand)) {
            sendCastFeedback(player, false, null, "aetherius.cast.no_wand");
            return;
        }
        if (action == 0) { // clear
            wand.clearRunes(stack);
            sendCastFeedback(player, true, null, "aetherius.inscribe.cleared");
            return;
        }
        if (runeOrdinal < 0 || runeOrdinal >= Rune.values().length) {
            return;
        }
        Rune rune = Rune.values()[runeOrdinal];
        PlayerMagicCapability magic = MagicComponents.get(player);
        if (!magic.knows(rune)) {
            sendCastFeedback(player, false, null, "aetherius.inscribe.unknown_rune");
            return;
        }
        RuneCombination next = wand.inscribe(stack, rune);
        sendCastFeedback(player, true, null, "aetherius.inscribe.added");
        var spell = SpellRegistry.resolve(next);
        if (spell != null) {
            player.sendMessage(net.minecraft.text.Text.translatable("aetherius.inscribe.preview",
                    net.minecraft.text.Text.translatable(spell.translationKey())), true);
        }
    }

    private static void handleChronicleAction(ServerPlayerEntity player, int action, String questId) {
        switch (action) {
            // Action 0 is "give me the current state", NOT "open the book". Replying with an
            // open packet here made the client recreate the screen every frame, which swallowed
            // every click and even ESC (the screen was replaced as fast as it closed).
            case 0 -> ChronicleSyncS2CPacket.send(player, MagicComponents.get(player));
            case 1 -> ChronicleManager.get().onObservatoryVisited(player); // debug/continuity check
            case 2 -> {
                Identifier id = Identifier.tryParse(questId);
                if (id != null) {
                    player.sendMessage(net.minecraft.text.Text.translatable("aetherius.chronicle.tracked",
                            net.minecraft.text.Text.translatable(
                                    ChronicleManager.get().quest(id) != null
                                            ? ChronicleManager.get().quest(id).titleKey()
                                            : "aetherius.chronicle.unknown")), true);
                }
            }
            default -> Aetherius.LOGGER.debug("Unknown chronicle action {} from {}", action, player.getName());
        }
    }

    /** Helper used by the wand item and boss scripting to build a packet from a pattern. */
    public static PacketByteBuf writeCombination(RuneCombination combination) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte((byte) combination.size());
        for (Rune rune : combination.runes()) {
            buf.writeByte((byte) rune.ordinal());
        }
        return buf;
    }

    /** Convenience for HUD-less debugging of school mastery values. */
    public static int masteryOf(PlayerMagicCapability magic, SpellSchool school) {
        return magic.getSchoolMastery(school);
    }
}
