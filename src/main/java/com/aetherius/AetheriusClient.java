package com.aetherius;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.Rune;
import com.aetherius.client.ClientEffects;
import com.aetherius.client.ClientNetworkHandlers;
import com.aetherius.client.hud.MagicHudOverlay;
import com.aetherius.client.hud.NoxThalBossBarOverlay;
import com.aetherius.client.render.AetherMantaModel;
import com.aetherius.client.render.AetherMantaRenderer;
import com.aetherius.client.render.AetherMobRenderer;
import com.aetherius.client.render.GravityAnchorRenderer;
import com.aetherius.client.render.NoxThalRenderer;
import com.aetherius.client.screen.ChronicleScreen;
import com.aetherius.network.InscribeRuneC2SPacket;
import com.aetherius.registry.ModEntities;
import com.aetherius.util.Aeth;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Client entry point: renderers, HUD overlays, keybinds and packet handlers.
 *
 * <h2>Keybinds</h2>
 * <ul>
 *   <li><b>K</b> - open the Chronicle.</li>
 *   <li><b>R</b> - inscribe the next known rune onto the held wand (cycles).</li>
 *   <li><b>Shift + R</b> - clear the wand's inscription.</li>
 * </ul>
 */
public final class AetheriusClient implements ClientModInitializer {

    private static final Identifier SENTINEL_TEXTURE = Aeth.id("textures/entity/void_sentinel.png");
    private static final Identifier SPECTER_TEXTURE = Aeth.id("textures/entity/void_specter.png");
    private static final Identifier ABOMINATION_TEXTURE = Aeth.id("textures/entity/void_warped_abomination.png");
    private static final Identifier WARDEN_TEXTURE = Aeth.id("textures/entity/elemental_warden.png");

    private static KeyBinding openChronicleKey;
    private static KeyBinding inscribeRuneKey;
    private static int runeCursor;

    @Override
    public void onInitializeClient() {
        ClientNetworkHandlers.register();
        com.aetherius.client.particle.AetherParticles.register();

        HudRenderCallback.EVENT.register(new MagicHudOverlay());
        HudRenderCallback.EVENT.register(new NoxThalBossBarOverlay());

        EntityModelLayerRegistry.registerModelLayer(AetherMantaModel.LAYER, AetherMantaModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.NOX_THAL, NoxThalRenderer::new);
        EntityRendererRegistry.register(ModEntities.AETHER_MANTA, AetherMantaRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRAVITY_ANCHOR, GravityAnchorRenderer::new);
        EntityRendererRegistry.register(ModEntities.VOID_SENTINEL, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.PLAYER, SENTINEL_TEXTURE, 0.6f));
        EntityRendererRegistry.register(ModEntities.VOID_SPECTER, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.PLAYER, SPECTER_TEXTURE, 0.5f));
        EntityRendererRegistry.register(ModEntities.VOID_WARPED_ABOMINATION, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.ZOMBIE, ABOMINATION_TEXTURE, 0.5f));
        EntityRendererRegistry.register(ModEntities.EMBERS_WARDEN, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.PLAYER, WARDEN_TEXTURE, 1.0f));
        EntityRendererRegistry.register(ModEntities.TIDES_WARDEN, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.PLAYER, WARDEN_TEXTURE, 1.0f));
        EntityRendererRegistry.register(ModEntities.STONES_WARDEN, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.PLAYER, WARDEN_TEXTURE, 1.0f));
        EntityRendererRegistry.register(ModEntities.STORMS_WARDEN, context ->
                new AetherMobRenderer<>(context, EntityModelLayers.PLAYER, WARDEN_TEXTURE, 1.0f));

        openChronicleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aetherius.chronicle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.aetherius"));
        inscribeRuneKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aetherius.inscribe", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.aetherius"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientEffects.tickShake();
            ClientEffects.tickDarkness();

            while (openChronicleKey.wasPressed()) {
                client.setScreen(new ChronicleScreen());
            }
            while (inscribeRuneKey.wasPressed() && client.player != null) {
                if (client.player.isSneaking()) {
                    InscribeRuneC2SPacket.clear();
                } else {
                    cycleRune(client);
                }
            }
            if (client.world == null) {
                ClientEffects.reset();
            }
        });
    }

    private void cycleRune(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        List<Rune> known = new ArrayList<>(MagicComponents.get(client.player).getUnlockedRunes());
        if (known.isEmpty()) {
            return;
        }
        known.sort(java.util.Comparator.comparingInt(Enum::ordinal));
        runeCursor = (runeCursor + 1) % known.size();
        Rune rune = known.get(runeCursor);
        InscribeRuneC2SPacket.append(rune);
        client.player.sendMessage(net.minecraft.text.Text.translatable("aetherius.inscribe.selected",
                net.minecraft.text.Text.translatable(rune.translationKey())
                        .formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)), true);
    }
}
