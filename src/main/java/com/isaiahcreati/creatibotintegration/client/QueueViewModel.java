package com.isaiahcreati.creatibotintegration.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure queue projection shared by the HUD and ordinary JVM tests. */
public final class QueueViewModel {

    private QueueViewModel() {}

    public enum Category {
        MINIGAME("GAME", 0x55FFFF),
        VISUAL_EFFECT("FX", 0xFF55FF),
        TAUNT("TAUNT", 0xFFAA00),
        BUFF("BUFF", 0x55FF55);

        private final String label;
        private final int color;

        Category(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String label() { return label; }
        public int color() { return color; }
    }

    public record Entry(String id, String displayName, String redeemerName, int durationSeconds) {}

    public record DisplayEntry(Category category, Entry entry) {}

    public static int totalSize(List<Entry> minigames,
                                List<Entry> effects,
                                List<Entry> taunts,
                                List<Entry> buffs) {
        return minigames.size() + effects.size() + taunts.size() + buffs.size();
    }

    public static List<DisplayEntry> visibleEntries(int limit,
                                                    List<Entry> minigames,
                                                    List<Entry> effects,
                                                    List<Entry> taunts,
                                                    List<Entry> buffs) {
        if (limit <= 0) return Collections.emptyList();

        int total = totalSize(minigames, effects, taunts, buffs);
        List<DisplayEntry> entries = new ArrayList<>(Math.min(limit, total));
        append(entries, minigames, Category.MINIGAME, limit);
        append(entries, effects, Category.VISUAL_EFFECT, limit);
        append(entries, taunts, Category.TAUNT, limit);
        append(entries, buffs, Category.BUFF, limit);
        return entries;
    }

    private static void append(List<DisplayEntry> target,
                               List<Entry> source,
                               Category category,
                               int limit) {
        for (Entry entry : source) {
            if (target.size() >= limit) return;
            target.add(new DisplayEntry(category, entry));
        }
    }
}
