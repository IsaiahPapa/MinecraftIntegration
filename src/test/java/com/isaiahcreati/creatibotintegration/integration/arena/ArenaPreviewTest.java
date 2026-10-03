package com.isaiahcreati.creatibotintegration.integration.arena;

import com.isaiahcreati.creatibotintegration.integration.DropperArena;
import com.isaiahcreati.creatibotintegration.integration.ParkourCourse;
import com.isaiahcreati.creatibotintegration.integration.SumoArena;
import com.isaiahcreati.creatibotintegration.integration.TntRunArena;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * Renders each arena to build/arena-previews. Off by default; run with
 * {@code ./gradlew test -Darena.previews=true --tests '*ArenaPreviewTest'}.
 */
class ArenaPreviewTest {

    private static File out(String name) {
        Assumptions.assumeTrue(Boolean.getBoolean("arena.previews"), "previews disabled");
        return new File(System.getProperty("arena.previewDir"), name);
    }

    @Test
    void tntRun() throws IOException {
        MemoryCanvas canvas = new MemoryCanvas();
        TntRunArena.build(canvas, 20, 2);
        ArenaRenderer.renderIso(canvas, p -> true, out("tntrun.png"));
        // Cut away the near half to see the floors inside the wall.
        ArenaRenderer.renderIso(canvas, p -> (p.getX() - 200) + (p.getZ() - 0) <= 0, out("tntrun-cutaway.png"));
        ArenaRenderer.renderTop(canvas, p -> p.getY() <= 66, out("tntrun-top.png"));
    }

    @Test
    void dropper() throws IOException {
        MemoryCanvas canvas = new MemoryCanvas();
        DropperArena.build(canvas, 2);
        ArenaRenderer.renderIso(canvas, p -> true, out("dropper.png"));
        ArenaRenderer.renderIso(canvas, p -> (p.getX() - 200) + (p.getZ() - 200) <= 0, out("dropper-cutaway.png"));
        ArenaRenderer.renderIso(canvas, p -> p.getY() >= 128, out("dropper-top.png"));
        ArenaRenderer.renderIso(canvas, p -> p.getY() <= 64 && (p.getX() - 200) + (p.getZ() - 200) <= 2, out("dropper-bottom.png"));
        ArenaRenderer.renderTop(canvas, p -> p.getY() <= 60, out("dropper-floor-map.png"));
    }

    @Test
    void sumo() throws IOException {
        MemoryCanvas canvas = new MemoryCanvas();
        SumoArena.build(canvas, 10);
        ArenaRenderer.renderIso(canvas, p -> true, out("sumo.png"));
        ArenaRenderer.renderIso(canvas, p -> p.getY() < 78, out("sumo-no-roof.png"));
        ArenaRenderer.renderIso(canvas, p -> p.getY() < 78 && (p.getX() - 300) + (p.getZ() - 300) <= 0, out("sumo-cutaway.png"));
    }

    @Test
    void parkour() throws IOException {
        for (int version = 1; version <= 3; version++) {
            MemoryCanvas canvas = new MemoryCanvas();
            ParkourCourse.build(canvas, version);
            ArenaRenderer.renderIso(canvas, p -> true, out("parkour-v" + version + ".png"));
            ArenaRenderer.renderTop(canvas, p -> p.getY() >= 60, out("parkour-v" + version + "-top.png"));
        }
    }
}
