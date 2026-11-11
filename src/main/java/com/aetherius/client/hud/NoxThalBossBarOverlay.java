package com.aetherius.client.hud;

import com.aetherius.client.ClientEffects;
import com.aetherius.entity.NoxThalEntity;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Mth;

/**
 * Custom Nox'Thal boss bar + Supernova darkness overlay.
 *
 * <h2>Why not the vanilla boss bar?</h2>
 * {@code BossBar.Color} is a fixed enum (PINK/BLUE/RED/GREEN/YELLOW/PURPLE/WHITE), so
 * per-phase RGB (violet -> cyan -> solar orange) is impossible with vanilla rendering. The
 * server still owns a {@code ServerBossBar} for accessibility/tracking, but the actual pixels
 * here are drawn from the live entity's health and the phase index pushed by
 * {@code BossEventS2CPacket}.
 *
 * <h2>Supernova</h2>
 * Phase 3 draws a near-opaque black overlay with only glowing rune silhouettes visible, so
 * Rune Lanterns placed in the arena are the only navigational reference.
 */
public final class NoxThalBossBarOverlay implements HudRenderCallback {

    private static final int BAR_WIDTH = 220;
    private static final int BAR_HEIGHT = 12;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        if (ClientEffects.isDark()) {
            drawSupernova(context);
        }

        NoxThalEntity boss = findBoss(client);
        if (boss == null) {
            return;
        }

        int phase = ClientEffects.bossPhase(boss.getId());
        int rgb = ClientEffects.phaseColor(phase);
        float healthRatio = Mth.clamp(boss.getHealth() / boss.getMaxHealth(), 0f, 1f);

        int screenWidth = context.getScaledWindowWidth();
        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = 16;
        TextRenderer textRenderer = client.textRenderer;

        // Frame + background.
        context.fill(x - 2, y - 2, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, 0xFF140A22);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF241238);
        // Health fill with a soft inner glow.
        int filled = (int) (BAR_WIDTH * healthRatio);
        context.fill(x, y, x + filled, y + BAR_HEIGHT, 0xFF000000 | rgb);
        context.fill(x, y + BAR_HEIGHT - 3, x + filled, y + BAR_HEIGHT, 0x66FFFFFF);
        // Phase notch markers at 67% and 34%.
        for (float notch : new float[]{0.34f, 0.67f}) {
            int notchX = x + (int) (BAR_WIDTH * notch);
            context.fill(notchX, y - 1, notchX + 1, y + BAR_HEIGHT + 1, 0x99FFFFFF);
        }

        Text name = Text.translatable("entity.aetherius.nox_thal");
        context.drawTextWithShadow(textRenderer, name, x, y - 13, 0xFFF2E6FF);
        Text phaseLabel = Text.translatable("aetherius.boss.nox_thal.phase" + (phase + 1));
        context.drawTextWithShadow(textRenderer, phaseLabel,
                x + BAR_WIDTH - textRenderer.getWidth(phaseLabel), y - 13, 0xFF000000 | rgb);

        String health = String.format("%.0f / %.0f", boss.getHealth(), boss.getMaxHealth());
        context.drawTextWithShadow(textRenderer, Text.literal(health),
                x + BAR_WIDTH / 2 - textRenderer.getWidth(health) / 2, y + BAR_HEIGHT + 3, 0xFFB9A8D8);

        if (phase == 1 && boss.hasActiveAnchors()) {
            Text hint = Text.translatable("aetherius.boss.nox_thal.anchor_hint");
            context.drawTextWithShadow(textRenderer, hint,
                    screenWidth / 2 - textRenderer.getWidth(hint) / 2, y + BAR_HEIGHT + 14, 0xFF7FE4FF);
        }
    }

    private void drawSupernova(DrawContext context) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int alpha = (int) (ClientEffects.darknessAlpha() * 255f);
        if (alpha <= 0) {
            return;
        }
        context.fill(0, 0, width, height, (alpha << 24));
        // Faint rune glimmers so the overlay never looks like a broken render.
        long time = System.currentTimeMillis() / 40L;
        for (int i = 0; i < 12; i++) {
            int gx = (int) ((i * 811L + time * (i % 3 + 1)) % Math.max(1, width));
            int gy = (int) ((i * 523L + time * (i % 4 + 1)) % Math.max(1, height));
            int glow = 40 + (int) (Math.sin(time * 0.3 + i) * 30);
            context.fill(gx, gy, gx + 2, gy + 2, (Math.min(255, glow) << 24) | 0x9B4DFF);
        }
        Text warning = Text.translatable("aetherius.boss.nox_thal.supernova");
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, warning,
                width / 2 - MinecraftClient.getInstance().textRenderer.getWidth(warning) / 2,
                height / 2 + 40, 0xFFFF8A3D);
    }

    private NoxThalEntity findBoss(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }
        var bosses = client.world.getEntitiesByClass(NoxThalEntity.class,
                client.player.getBoundingBox().expand(128.0), e -> e.isAlive());
        NoxThalEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (NoxThalEntity boss : bosses) {
            double distance = boss.squaredDistanceTo(client.player);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = boss;
            }
        }
        return nearest;
    }
}
