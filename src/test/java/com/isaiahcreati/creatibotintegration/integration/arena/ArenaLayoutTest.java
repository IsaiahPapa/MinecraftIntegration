package com.isaiahcreati.creatibotintegration.integration.arena;

import com.isaiahcreati.creatibotintegration.integration.DropperArena;
import com.isaiahcreati.creatibotintegration.integration.ParkourCourse;
import com.isaiahcreati.creatibotintegration.integration.SumoArena;
import com.isaiahcreati.creatibotintegration.integration.TntRunArena;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the gameplay-critical parts of each arena layout without a server. */
class ArenaLayoutTest {

    /** Solid ground under the spawn, and room for the player and the countdown cage. */
    private static void assertSafeSpawn(MemoryCanvas canvas, BlockPos feet) {
        assertFalse(canvas.get(feet.getX(), feet.getY() - 1, feet.getZ()).isAir(), "spawn needs a floor at " + feet);
        for (BlockPos pos : new BlockPos[]{feet, feet.above(), feet.north(), feet.south(), feet.east(), feet.west(),
                feet.north().above(), feet.south().above(), feet.east().above(), feet.west().above()}) {
            assertTrue(canvas.isAir(pos.getX(), pos.getY(), pos.getZ()), "spawn space blocked at " + pos);
        }
    }

    @Test
    void tntRunFloorsAreWalledAndSpawnIsSafe() {
        for (int floors = 1; floors <= 3; floors++) {
            MemoryCanvas canvas = new MemoryCanvas();
            TntRunArena.build(canvas, 20, floors);
            assertSafeSpawn(canvas, new BlockPos(200, 65, 0));
            for (int f = 0; f < floors; f++) {
                int floorY = TntRunArena.FLOOR_Y_LEVELS_ALL[f];
                assertFalse(canvas.isAir(207, floorY, 0), "floor " + f + " missing walkable blocks");
                assertFalse(canvas.isAir(210, floorY + 1, 0), "floor " + f + " is not walled in");
            }
            // Nothing between the floors for a falling player to catch on.
            assertTrue(canvas.isAir(203, TntRunArena.FLOOR_Y_LEVELS_ALL[floors - 1] - 1, 3));
        }
    }

    @Test
    void dropperShaftIsClearAndPoolIsFlush() {
        MemoryCanvas canvas = new MemoryCanvas();
        DropperArena.build(canvas, 2);
        assertSafeSpawn(canvas, new BlockPos(200, 141, 200));

        // An unobstructed drop from just below the launch block to the cave floor.
        for (int y = 61; y < 140; y++) {
            for (int x = 194; x <= 206; x++) {
                for (int z = 194; z <= 206; z++) {
                    double dx = x - 200, dz = z - 200;
                    if (dx * dx + dz * dz < 6 * 6) {
                        assertTrue(canvas.isAir(x, y, z), "obstruction in shaft at " + new BlockPos(x, y, z));
                    }
                }
            }
        }

        // The 2x2 pool sits at x 195-196, z 200-201, with nothing standing
        // above water level around it for a player to land on.
        for (int x = 195; x <= 196; x++) {
            for (int z = 200; z <= 201; z++) {
                assertTrue(canvas.get(x, 55, z).is(Blocks.WATER));
            }
        }
        for (int x = 194; x <= 197; x++) {
            for (int z = 199; z <= 202; z++) {
                assertTrue(canvas.isAir(x, 56, z), "lip above pool rim at " + new BlockPos(x, 56, z));
            }
        }
    }

    @Test
    void sumoGapIsOpenAndRoofShadesTheArena() {
        int radius = 10;
        MemoryCanvas canvas = new MemoryCanvas();
        SumoArena.build(canvas, radius);
        assertSafeSpawn(canvas, new BlockPos(300, 65, 300));

        for (int x = 300 - 16; x <= 300 + 16; x++) {
            for (int z = 300 - 16; z <= 300 + 16; z++) {
                double distance = Math.sqrt((x - 300) * (x - 300) + (z - 300) * (z - 300));
                if (distance > radius + 0.5 && distance < radius + 1.9) {
                    // Anything pushed off the edge must fall into the moat.
                    for (int y = 57; y <= 67; y++) {
                        assertTrue(canvas.isAir(x, y, z), "knock-off gap blocked at " + new BlockPos(x, y, z));
                    }
                }
                if (distance <= radius + 3) {
                    // Undead burn in sunlight, so every column needs cover.
                    boolean covered = false;
                    for (int y = 70; y <= 96 && !covered; y++) {
                        covered = !canvas.isAir(x, y, z);
                    }
                    assertTrue(covered, "arena column open to the sky at " + x + "," + z);
                }
            }
        }
    }

    @Test
    void parkourStartsSafelyAndFinishBeaconSeesTheSky() {
        int[][] beacons = {{0, 66, 42}, {0, 69, 48}, {2, 69, 53}};
        for (int version = 1; version <= 3; version++) {
            MemoryCanvas canvas = new MemoryCanvas();
            ParkourCourse.build(canvas, version);
            assertSafeSpawn(canvas, new BlockPos(0, 65, 0));

            int[] beacon = beacons[version - 1];
            assertTrue(canvas.get(beacon[0], beacon[1], beacon[2]).is(Blocks.BEACON));
            for (int y = beacon[1] + 2; y <= 96; y++) {
                assertTrue(canvas.isAir(beacon[0], y, beacon[2]), "beacon beam blocked at y=" + y);
            }
            assertEquals(9, countIron(canvas, beacon), "beacon needs a full 3x3 base");
        }
    }

    private static int countIron(MemoryCanvas canvas, int[] beacon) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (canvas.get(beacon[0] + dx, beacon[1] - 1, beacon[2] + dz).is(Blocks.IRON_BLOCK)) count++;
            }
        }
        return count;
    }
}
