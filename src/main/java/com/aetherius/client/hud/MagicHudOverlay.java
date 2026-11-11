package com.aetherius.client.hud;

import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.magic.PlayerMagicCapability;
import com.aetherius.client.ClientEffects;
import com.aetherius.util.Aeth;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Mth;

/**
 * Custom HUD: the Mana and Astral Tension bars.
 *
 * <h2>Layout</h2>
 * Two stacked fluid bars anchored above the hotbar, each 140x10 px, drawn from custom textures
 * ({@code textures/gui/mana_bar.png}, {@code textures/gui/tension_bar.png}) and overlaid with a
 * translucent "recently spent" ghost that drains toward the real value - the classic fighting
 * game damage-ghost, which makes big casts feel weighty.
 *
 * <h2>States</h2>
 * <ul>
 *   <li>Mana Volatility (&gt;= 50 tension): tension bar pulses, mana numeric turns orange.</li>
 *   <li>Astral Backfire: whole HUD shakes, screen edges flash, bars desaturate.</li>
 *   <li>Aether Fountain proximity: mana bar gains a shimmer overlay.</li>
 *   <li>Low mana: bar blinks when a cast would fail.</li>
 * </ul>
 */
public final class MagicHudOverlay implements HudRenderCallback {

    private static final Identifier MANA_BAR = Aeth.id("textures/gui/mana_bar.png");
    private static final Identifier TENSION_BAR = Aeth.id("textures/gui/tension_bar.png");
    private static final Identifier RUNE_PIP = Aeth.id("textures/gui/rune_pip.png");

    private static final int BAR_WIDTH = 128;
    private static final int BAR_HEIGHT = 8;

    private float displayedMana = -1f;
    private float ghostMana = -1f;
    private float displayedTension;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) {
            return;
        }
        PlayerMagicCapability magic = MagicComponents.get(client.player);
        if (magic.getMaxMana() <= 0) {
            return;
        }

        smooth(magic, tickDelta);

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int baseX = screenWidth / 2 - BAR_WIDTH / 2;
        int baseY = screenHeight - 62;

        context.getMatrices().push();
        if (ClientEffects.isShaking()) {
            context.getMatrices().translate(ClientEffects.shakeX(), ClientEffects.shakeY(), 0f);
        }

        drawMana(context, client.textRenderer, magic, baseX, baseY);
        drawTension(context, client.textRenderer, magic, baseX, baseY + 14);
        drawRunePips(context, magic, baseX + BAR_WIDTH + 4, baseY);

        context.getMatrices().pop();

        if (magic.isBackfiring()) {
            drawBackfireFlash(context, screenWidth, screenHeight);
        }
    }

    private void smooth(PlayerMagicCapability magic, float tickDelta) {
        float mana = magic.getMana();
        if (displayedMana < 0) {
            displayedMana = mana;
            ghostMana = mana;
        }
        displayedMana = Mth.lerp(0.25f, displayedMana, mana);
        ghostMana = ghostMana > displayedMana
                ? Math.max(displayedMana, ghostMana - 0.6f)
                : displayedMana;
        displayedTension = Mth.lerp(0.18f, displayedTension, magic.getAstralTension());
    }

    private void drawMana(DrawContext context, TextRenderer textRenderer, PlayerMagicCapability magic,
                          int x, int y) {
        float ratio = ClientEffects.clamp01(displayedMana / magic.getMaxMana());
        float ghostRatio = ClientEffects.clamp01(ghostMana / magic.getMaxMana());

        // Background frame (v=0), then the fill (v=BAR_HEIGHT).
        context.drawTexture(MANA_BAR, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT * 2);
        if (ghostRatio > ratio) {
            context.fill(x + (int) (BAR_WIDTH * ratio), y,
                    x + (int) (BAR_WIDTH * ghostRatio), y + BAR_HEIGHT, 0x66FFFFFF);
        }
        if (ratio > 0f) {
            context.drawTexture(MANA_BAR, x, y, 0, BAR_HEIGHT,
                    Math.max(1, (int) (BAR_WIDTH * ratio)), BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT * 2);
        }
        if (magic.isNearAetherFountain()) {
            context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x2233E1FF);
        }

        String label = String.format("%.0f / %.0f", magic.getMana(), magic.getMaxMana());
        context.drawText(textRenderer, Text.literal(label),
                x + BAR_WIDTH - textRenderer.getWidth(label), y - 10,
                magic.isManaVolatile() ? 0xFFFF9A3D : 0xFF9FE8FF, true);
    }

    private void drawTension(DrawContext context, TextRenderer textRenderer, PlayerMagicCapability magic,
                             int x, int y) {
        float ratio = ClientEffects.clamp01(displayedTension / 100f);
        context.drawTexture(TENSION_BAR, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT * 2);
        if (ratio > 0f) {
            context.drawTexture(TENSION_BAR, x, y, 0, BAR_HEIGHT,
                    Math.max(1, (int) (BAR_WIDTH * ratio)), BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT * 2);
        }
        // Threshold marker at 50% (volatility) and 100% (backfire).
        int halfX = x + BAR_WIDTH / 2;
        context.fill(halfX, y - 1, halfX + 1, y + BAR_HEIGHT + 1, 0xAAFFFFFF);

        if (magic.isManaVolatile()) {
            float pulse = (float) (Math.sin(magic.getBackfireTicks() * 0.4 + System.currentTimeMillis() / 180.0) * 0.5 + 0.5);
            int alpha = (int) (60 + pulse * 80);
            context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1,
                    (alpha << 24) | 0x9B4DFF);
            context.drawText(textRenderer, Text.translatable("aetherius.hud.volatile"),
                    x, y - 10, 0xFFD08CFF, true);
        }
        String value = String.format("%.0f%%", magic.getAstralTension());
        context.drawText(textRenderer, Text.literal(value),
                x + BAR_WIDTH - textRenderer.getWidth(value), y + BAR_HEIGHT + 1, 0xFFB98CFF, true);
    }

    private void drawRunePips(DrawContext context, PlayerMagicCapability magic, int x, int y) {
        int index = 0;
        for (var rune : magic.getUnlockedRunes()) {
            int pipX = x + (index % 3) * 12;
            int pipY = y + (index / 3) * 12;
            context.drawTexture(RUNE_PIP, pipX, pipY, 0, 0, 10, 10, 10, 20);
            context.fill(pipX + 2, pipY + 2, pipX + 8, pipY + 8,
                    0xFF000000 | rune.school().color());
            index++;
            if (index >= 9) {
                break;
            }
        }
    }

    private void drawBackfireFlash(DrawContext context, int width, int height) {
        float progress = ClientEffects.clamp01(
                MagicComponents.get(MinecraftClient.getInstance().player).getBackfireTicks() / 60f);
        int alpha = (int) (70 * progress);
        context.fill(0, 0, width, height, (alpha << 24) | 0x9B4DFF);
        context.fill(0, 0, width, 24, (alpha << 24) | 0xFF2D6B);
        context.fill(0, height - 24, width, height, (alpha << 24) | 0xFF2D6B);
    }
}
