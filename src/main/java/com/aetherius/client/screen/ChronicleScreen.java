package com.aetherius.client.screen;

import com.aetherius.api.chronicle.Chapter;
import com.aetherius.api.chronicle.ChronicleRegistry;
import com.aetherius.api.chronicle.PlayerChronicle;
import com.aetherius.api.chronicle.Quest;
import com.aetherius.api.chronicle.QuestObjective;
import com.aetherius.api.chronicle.QuestReward;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.network.ChronicleActionC2SPacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game <b>Chronicle</b>: a lore book and quest tracker in one screen.
 *
 * <h2>Layout</h2>
 * <pre>
 * +---------------------------------------------------------------+
 * | ACT I | ACT II | ACT III | APPENDIX |            (tab strip)   |
 * +-----------------+---------------------------------------------+
 * | quest list      | selected quest: title, lore, objectives,     |
 * | (with [x]/[ ])  | rewards, milestone state                    |
 * |                 |                                             |
 * +-----------------+---------------------------------------------+
 * | current Act: 2   runes known: 7    tension: 42%   (footer)    |
 * +---------------------------------------------------------------+
 * </pre>
 *
 * <p>All data is read from the client-side Chronicle mirror, which the server pushes on every
 * change. The screen is read-only except for the refresh/track actions, so it can never desync
 * progression.</p>
 */
public class ChronicleScreen extends Screen {

    private static final int BG = 0xF00D0818;
    private static final int PANEL = 0xFF170F28;
    private static final int ACCENT = 0xFF9B4DFF;
    private static final int TEXT = 0xFFE8E0FF;
    private static final int MUTED = 0xFF9A8FB8;
    private static final int DONE = 0xFF63E6A8;

    private Chapter chapter = Chapter.ACT_I;
    private Quest selected;

    private final List<int[]> tabBounds = new ArrayList<>();
    private final List<Chapter> tabChapters = new ArrayList<>();
    private final List<int[]> questBounds = new ArrayList<>();
    private final List<Quest> visibleQuests = new ArrayList<>();

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public ChronicleScreen() {
        super(Text.translatable("aetherius.chronicle.title"));
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(460, this.width - 40);
        this.panelHeight = Math.min(260, this.height - 40);
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aetherius.chronicle.close"),
                        button -> this.close())
                .dimensions(this.width / 2 - 40, panelTop + panelHeight - 24, 80, 18).build());

        // Ask the server for a fresh snapshot every time the book opens.
        ChronicleActionC2SPacket.send(ChronicleActionC2CPacket.ACTION_OPEN, "");

        if (selected == null) {
            List<Quest> quests = ChronicleRegistry.questsFor(chapter);
            selected = quests.isEmpty() ? null : quests.get(0);
        }
    }

    /** Called when a fresh ChronicleSync arrives while the screen is open. */
    public void refresh() {
        // Data is pulled live in render(); nothing to rebuild, but keep the hook for future
        // animated transitions.
    }

    private PlayerChronicle chronicle() {
        if (this.client == null || this.client.player == null) {
            return new PlayerChronicle();
        }
        return MagicComponents.get(this.client.player).getChronicle();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        PlayerChronicle chronicle = chronicle();

        // Outer frame.
        context.fill(panelLeft - 4, panelTop - 4, panelLeft + panelWidth + 4,
                panelTop + panelHeight + 4, BG);
        context.fill(panelLeft - 4, panelTop - 4, panelLeft + panelWidth + 4, panelTop - 2, ACCENT);

        Text title = Text.translatable("aetherius.chronicle.title").formatted(Formatting.BOLD);
        context.drawTextWithShadow(this.textRenderer, title, panelLeft, panelTop - 14, TEXT);

        drawTabs(context, mouseX, mouseY, chronicle);

        int listWidth = 150;
        int listTop = panelTop + 26;
        context.fill(panelLeft, listTop, panelLeft + listWidth, panelTop + panelHeight - 30, PANEL);
        context.fill(panelLeft + listWidth, listTop, panelLeft + listWidth + 1,
                panelTop + panelHeight - 30, ACCENT);

        drawQuestList(context, chronicle, panelLeft, listTop, listWidth - 6);

        int detailLeft = panelLeft + listWidth + 10;
        int detailWidth = panelWidth - listWidth - 14;
        drawQuestDetail(context, chronicle, detailLeft, listTop, detailWidth);

        drawFooter(context, chronicle);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY, PlayerChronicle chronicle) {
        tabBounds.clear();
        tabChapters.clear();
        Chapter[] chapters = {Chapter.ACT_I, Chapter.ACT_II, Chapter.ACT_III, Chapter.APPENDIX};
        int x = panelLeft;
        int y = panelTop + 4;
        for (Chapter candidate : chapters) {
            Text label = Text.translatable(candidate.titleKey());
            int tabWidth = this.textRenderer.getWidth(label) + 16;
            boolean active = candidate == chapter;
            boolean hovered = mouseX >= x && mouseX < x + tabWidth && mouseY >= y && mouseY < y + 18;
            boolean unlocked = candidate.act() == 0 || chronicle.act() >= candidate.act();
            int background = active ? ACCENT : hovered ? 0xFF2A1B44 : PANEL;
            context.fill(x, y, x + tabWidth, y + 18, background);
            context.drawTextWithShadow(this.textRenderer, label, x + 8, y + 5,
                    unlocked ? TEXT : MUTED);
            tabBounds.add(new int[]{x, y, tabWidth, 18});
            tabChapters.add(candidate);
            x += tabWidth + 4;
        }
    }

    private void drawQuestList(DrawContext context, PlayerChronicle chronicle, int x, int y, int width) {
        visibleQuests.clear();
        questBounds.clear();

        List<Quest> quests = ChronicleRegistry.questsFor(chapter);
        int rowY = y + 6;
        for (Quest quest : quests) {
            boolean unlocked = quest.isUnlocked(chronicle);
            if (!unlocked && quest.hidden()) {
                continue;
            }
            boolean complete = chronicle.isComplete(quest.id());
            visibleQuests.add(quest);

            int rowHeight = 24;
            boolean selectedRow = quest.equals(selected);
            if (selectedRow) {
                context.fill(x + 2, rowY - 2, x + width, rowY + rowHeight - 4, 0xFF2A1B44);
            }
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(complete ? "\u2714 " : "\u25CB "),
                    x + 6, rowY + 2, complete ? DONE : MUTED);
            Text label = Text.translatable(quest.titleKey());
            List<OrderedText> wrapped = this.textRenderer.wrapLines(label, width - 26);
            if (!wrapped.isEmpty()) {
                context.drawText(this.textRenderer, wrapped.get(0), x + 20, rowY + 2,
                        unlocked ? TEXT : MUTED, false);
            }
            questBounds.add(new int[]{x + 2, rowY - 2, width, rowHeight});
            rowY += rowHeight;
            if (rowY > panelTop + panelHeight - 50) {
                break;
            }
        }

        if (chapter == Chapter.APPENDIX) {
            int loreY = y + 6;
            context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("aetherius.chronicle.lore_header"), x + 6, loreY, ACCENT);
        }
    }

    private void drawQuestDetail(DrawContext context, PlayerChronicle chronicle, int x, int y, int width) {
        if (selected == null) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("aetherius.chronicle.no_selection"), x, y + 6, MUTED);
            return;
        }

        boolean complete = chronicle.isComplete(selected.id());
        Text heading = Text.translatable(selected.titleKey()).formatted(Formatting.BOLD);
        context.drawTextWithShadow(this.textRenderer, heading, x, y + 4, complete ? DONE : TEXT);

        int cursorY = y + 20;
        cursorY = drawWrapped(context, Text.translatable(selected.loreKey()).formatted(Formatting.GRAY),
                x, cursorY, width, 6);

        cursorY += 4;
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("aetherius.chronicle.objectives"), x, cursorY, ACCENT);
        cursorY += 12;

        for (QuestObjective objective : selected.objectives()) {
            int progress = objectiveProgress(objective, chronicle);
            boolean done = progress >= objective.target();
            String prefix = done ? "\u2714 " : "\u25CB ";
            String line = prefix + Text.translatable(objective.describeKey()).getString()
                    + "  [" + Math.min(progress, objective.target()) + "/" + objective.target() + "]";
            context.drawText(this.textRenderer, Text.literal(line), x + 4, cursorY,
                    done ? DONE : MUTED, false);
            cursorY += 11;
        }

        cursorY += 4;
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("aetherius.chronicle.rewards"), x, cursorY, ACCENT);
        cursorY += 12;
        for (QuestReward reward : selected.rewards()) {
            context.drawText(this.textRenderer, Text.literal("\u2726 " + Text.translatable(
                    reward.describeKey()).getString()), x + 4, cursorY, 0xFFFFD98A, false);
            cursorY += 11;
        }

        if (!selected.milestoneFlag().isBlank() && chronicle.hasMilestone(selected.milestoneFlag())) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("aetherius.chronicle.milestone_reached"),
                    x, cursorY + 4, DONE);
        }
    }

    private int objectiveProgress(QuestObjective objective, PlayerChronicle chronicle) {
        if (selected != null && chronicle.isComplete(selected.id())) {
            return objective.target();
        }
        // Counters are synced, so counter-backed objectives can animate client-side.
        int counter = chronicle.counter(objective.counterKey());
        if (counter > 0) {
            return Math.min(counter, objective.target());
        }
        return 0;
    }

    private int drawWrapped(DrawContext context, Text text, int x, int y, int width, int maxLines) {
        List<OrderedText> lines = this.textRenderer.wrapLines(text, width);
        int drawn = 0;
        for (OrderedText line : lines) {
            if (drawn >= maxLines) {
                break;
            }
            context.drawText(this.textRenderer, line, x, y + drawn * 10, TEXT, false);
            drawn++;
        }
        return y + drawn * 10;
    }

    private void drawFooter(DrawContext context, PlayerChronicle chronicle) {
        int y = panelTop + panelHeight - 22;
        context.fill(panelLeft, y - 4, panelLeft + panelWidth, y - 3, ACCENT);

        int runeCount = 0;
        float tension = 0f;
        if (this.client != null && this.client.player != null) {
            var magic = MagicComponents.get(this.client.player);
            runeCount = magic.getUnlockedRunes().size();
            tension = magic.getAstralTension();
        }

        Text footer = Text.translatable("aetherius.chronicle.footer",
                Text.literal(String.valueOf(chronicle.act())).formatted(Formatting.AQUA),
                Text.literal(String.valueOf(runeCount)).formatted(Formatting.LIGHT_PURPLE),
                Text.literal(String.format("%.0f%%", tension)).formatted(
                        tension >= 50 ? Formatting.RED : Formatting.GRAY));
        context.drawTextWithShadow(this.textRenderer, footer, panelLeft + 4, y + 2, MUTED);

        if (chronicle.isComplete(com.aetherius.util.Aeth.id("act3_nox"))) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("aetherius.chronicle.epilogue_badge"),
                    panelLeft + panelWidth - 88, y + 2, 0xFFFFD98A);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < tabBounds.size(); i++) {
            int[] bounds = tabBounds.get(i);
            if (mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2]
                    && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3]) {
                chapter = tabChapters.get(i);
                List<Quest> quests = ChronicleRegistry.questsFor(chapter);
                selected = quests.isEmpty() ? null : quests.get(0);
                return true;
            }
        }
        for (int i = 0; i < questBounds.size(); i++) {
            int[] bounds = questBounds.get(i);
            if (mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2]
                    && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3]
                    && i < visibleQuests.size()) {
                selected = visibleQuests.get(i);
                ChronicleActionC2SPacket.send(ChronicleActionC2CPacket.ACTION_TRACK,
                        selected.id().toString());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Footer counter used by the appendix page. */
    public static int loreUnlocked(PlayerChronicle chronicle) {
        return chronicle.unlockedLore().size();
    }

    /** Kept for the in-book search feature (Mth import keeps the intent documented). */
    private static int clampPage(int value, int max) {
        return Mth.clamp(value, 0, Math.max(0, max));
    }
}
