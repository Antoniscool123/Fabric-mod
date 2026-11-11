package com.aetherius.client.screen;

import java.util.List;

/**
 * Static, hand-written guide pages shown in the Chronicle's <b>GUIDE</b> tab.
 *
 * <h2>Why static text and not a data file?</h2>
 * A guide is documentation that ships with the build; keeping it in code (with the strings in
 * {@code en_us.json}) means {@code tools/check_lang.py} can prove every key exists, and it cannot
 * drift out of sync with the mechanics it describes the way a hand-copied JSON page would.
 *
 * <p>Every statement here is checked against the code it documents: the wand recipe is in
 * {@code data/aetherius/recipes/astral_wand.json}, the rune keys in {@code AetheriusClient}, the
 * anchor rules in {@code SkyRiftAnchorBlock}, and the boss phases in
 * {@code entity/boss/phase/*}. If a mechanic changes, change the paragraph with it.</p>
 */
public final class GuidePages {

    /** One titled page of the guide. {@code paragraphs} are translation keys, in reading order. */
    public record Section(String id, String titleKey, List<String> paragraphs) {
    }

    public static final List<Section> SECTIONS = List.of(
            new Section("start", "aetherius.guide.start.title", List.of(
                    "aetherius.guide.start.p1",
                    "aetherius.guide.start.p2",
                    "aetherius.guide.start.p3",
                    "aetherius.guide.start.p4")),
            new Section("runes", "aetherius.guide.runes.title", List.of(
                    "aetherius.guide.runes.p1",
                    "aetherius.guide.runes.p2",
                    "aetherius.guide.runes.p3",
                    "aetherius.guide.runes.p4")),
            new Section("world", "aetherius.guide.world.title", List.of(
                    "aetherius.guide.world.p1",
                    "aetherius.guide.world.p2",
                    "aetherius.guide.world.p3",
                    "aetherius.guide.world.p4")),
            new Section("boss", "aetherius.guide.boss.title", List.of(
                    "aetherius.guide.boss.p1",
                    "aetherius.guide.boss.p2",
                    "aetherius.guide.boss.p3",
                    "aetherius.guide.boss.p4")),
            new Section("craft", "aetherius.guide.craft.title", List.of(
                    "aetherius.guide.craft.p1",
                    "aetherius.guide.craft.p2",
                    "aetherius.guide.craft.p3")));

    private GuidePages() {
    }

    public static Section byIndex(int index) {
        if (SECTIONS.isEmpty()) {
            throw new IllegalStateException("guide has no sections");
        }
        return SECTIONS.get(Math.max(0, Math.min(index, SECTIONS.size() - 1)));
    }
}
