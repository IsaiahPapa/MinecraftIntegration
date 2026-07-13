package com.isaiahcreati.creatibotintegration.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueViewModelTest {

    @Test
    void projectsEveryQueueCategory() {
        List<QueueViewModel.DisplayEntry> visible = QueueViewModel.visibleEntries(
                4,
                List.of(entry("parkour", "Parkour", "Alice")),
                List.of(entry("crt", "Monitor Downgrade", "Bob")),
                List.of(entry("punch", "Smack", "Casey")),
                List.of(entry("frostbite", "Frostbite", "Dana"))
        );

        assertEquals(List.of(
                        QueueViewModel.Category.MINIGAME,
                        QueueViewModel.Category.VISUAL_EFFECT,
                        QueueViewModel.Category.TAUNT,
                        QueueViewModel.Category.BUFF),
                visible.stream().map(QueueViewModel.DisplayEntry::category).toList());
        assertEquals("Dana", visible.get(3).entry().redeemerName());
    }

    @Test
    void projectionStaysCappedWithoutLosingTotalCount() {
        List<QueueViewModel.Entry> games = List.of(
                entry("parkour", "Parkour", "One"),
                entry("tntrun", "TNT Run", "Two"),
                entry("dropper", "Dropper", "Three"));
        List<QueueViewModel.Entry> effects = List.of(entry("crt", "Monitor Downgrade", "Four"));
        List<QueueViewModel.Entry> taunts = List.of(entry("punch", "Smack", "Five"));
        List<QueueViewModel.Entry> buffs = List.of(entry("frostbite", "Frostbite", "Six"));

        assertEquals(6, QueueViewModel.totalSize(games, effects, taunts, buffs));
        assertEquals(4, QueueViewModel.visibleEntries(4, games, effects, taunts, buffs).size());
        assertEquals(0, QueueViewModel.visibleEntries(0, games, effects, taunts, buffs).size());
    }

    @Test
    void emptyProjectionIsStable() {
        assertEquals(0, QueueViewModel.totalSize(List.of(), List.of(), List.of(), List.of()));
        assertEquals(List.of(), QueueViewModel.visibleEntries(4, List.of(), List.of(), List.of(), List.of()));
    }

    private static QueueViewModel.Entry entry(String id, String name, String redeemer) {
        return new QueueViewModel.Entry(id, name, redeemer, 15);
    }
}
