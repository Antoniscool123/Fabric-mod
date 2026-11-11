package com.aetherius.api.chronicle;

/**
 * A Chronicle chapter = one Act of the story. Chapters gate world content:
 * Act II unlocks Aspect Corruption, Act III unlocks the Sky-Rift.
 */
public enum Chapter {
    ACT_I("act_i", 1, "aetherius.chronicle.chapter.act_i"),
    ACT_II("act_ii", 2, "aetherius.chronicle.chapter.act_ii"),
    ACT_III("act_iii", 3, "aetherius.chronicle.chapter.act_iii"),
    APPENDIX("appendix", 0, "aetherius.chronicle.chapter.appendix");

    private final String id;
    private final int act;
    private final String titleKey;

    Chapter(String id, int act, String titleKey) {
        this.id = id;
        this.act = act;
        this.titleKey = titleKey;
    }

    public String id() {
        return id;
    }

    public int act() {
        return act;
    }

    public String titleKey() {
        return titleKey;
    }

    public static Chapter byId(String id) {
        for (Chapter chapter : values()) {
            if (chapter.id.equals(id)) {
                return chapter;
            }
        }
        return APPENDIX;
    }
}
