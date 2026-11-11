package com.aetherius.client;

import com.aetherius.api.chronicle.ChronicleRegistry;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.api.magic.Rune;
import com.aetherius.api.magic.SpellSchool;
import com.aetherius.client.screen.ChronicleScreen;
import com.aetherius.network.AetherNetworking;
import com.aetherius.network.BossEventS2CPacket;
import com.aetherius.network.CastFeedbackS2CPacket;
import com.aetherius.network.ChronicleSyncS2CPacket;
import com.aetherius.network.MagicSyncS2CPacket;
import com.aetherius.network.RuneBitmask;
import com.aetherius.network.ScreenShakeS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-bound packet handlers.
 *
 * <p>All handlers read the buffer on the network thread and only touch game state inside
 * {@code client.execute(...)}. The capability the handlers write into is the client mirror
 * created by {@code PlayerEntityMixin}; the HUD, gravity mixin and boss bar all read from it,
 * which means there is exactly one client-side source of truth.</p>
 */
public final class ClientNetworkHandlers {

    private ClientNetworkHandlers() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AetherNetworking.MAGIC_SYNC,
                (client, handler, buf, sender) -> {
                    MagicSyncS2CPacket.MagicSnapshot snapshot = MagicSyncS2CPacket.read(buf);
                    client.execute(() -> applySnapshot(client, snapshot));
                });

        ClientPlayNetworking.registerGlobalReceiver(AetherNetworking.CHRONICLE_SYNC,
                (client, handler, buf, sender) -> {
                    var nbt = ChronicleSyncS2CPacket.read(buf);
                    client.execute(() -> {
                        if (client.player == null) {
                            return;
                        }
                        var magic = MagicComponents.get(client.player);
                        magic.getChronicle().readNbt(nbt);
                        if (client.currentScreen instanceof ChronicleScreen screen) {
                            screen.refresh();
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(AetherNetworking.CAST_FEEDBACK,
                (client, handler, buf, sender) -> {
                    CastFeedbackS2CPacket.Feedback feedback = CastFeedbackS2CPacket.read(buf);
                    client.execute(() -> {
                        if (client.player == null) {
                            return;
                        }
                        Text message = Text.translatable(feedback.messageKey())
                                .formatted(feedback.success() ? Formatting.AQUA : Formatting.RED);
                        client.player.sendMessage(message, true);
                        if (!feedback.success()) {
                            ClientEffects.triggerShake(3, 6);
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(AetherNetworking.SCREEN_SHAKE,
                (client, handler, buf, sender) -> {
                    ScreenShakeS2CPacket.Shake shake = ScreenShakeS2CPacket.read(buf);
                    client.execute(() -> ClientEffects.triggerShake(shake.intensity(), shake.durationTicks()));
                });

        ClientPlayNetworking.registerGlobalReceiver(AetherNetworking.BOSS_EVENT,
                (client, handler, buf, sender) -> {
                    BossEventS2CPacket.BossEvent event = BossEventS2CPacket.read(buf);
                    client.execute(() -> ClientEffects.onBossEvent(event));
                });

        ClientPlayNetworking.registerGlobalReceiver(AetherNetworking.OPEN_CHRONICLE,
                (client, handler, buf, sender) -> client.execute(() -> {
                    ChronicleRegistry.bootstrap();
                    client.setScreen(new ChronicleScreen());
                }));
    }

    private static void applySnapshot(MinecraftClient client, MagicSyncS2CPacket.MagicSnapshot snapshot) {
        if (client.player == null) {
            return;
        }
        PlayerMagicCapability magic = MagicComponents.get(client.player);
        magic.setMaxMana(snapshot.maxMana());
        magic.setMana(snapshot.mana());
        magic.setAstralTension(snapshot.tension());
        for (Rune rune : RuneBitmask.fromMask(snapshot.runeMask())) {
            magic.unlockRune(rune);
        }
        SpellSchool[] schools = SpellSchool.values();
        for (int i = 0; i < schools.length && i < snapshot.mastery().length; i++) {
            magic.setSchoolMastery(schools[i], snapshot.mastery()[i]);
        }
        // Mirror the server's boss/dimension states so the mixins agree on both sides.
        if (snapshot.gravityInversionTicks() > 0) {
            magic.setGravityInversion(snapshot.gravityInversionTicks());
        }
        if (snapshot.elytraLockTicks() > 0) {
            magic.setElytraLock(snapshot.elytraLockTicks());
        }
        if (snapshot.backfireTicks() > 0) {
            ClientEffects.triggerShake(14, snapshot.backfireTicks());
        }
    }
}
