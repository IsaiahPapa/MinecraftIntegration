package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class ParkourCourse {

    private static final String TEXT_MARKER_TAG = "creati_parkour_text";
    private record FloatingLabel(BlockPos pos, Component text) {}

    /** Inclusive block area a player stands in (feet level) to finish. */
    private record FinishPad(int minX, int maxX, int feetY, int minZ, int maxZ) {
        boolean contains(BlockPos feet) {
            return feet.getY() == feetY
                    && feet.getX() >= minX && feet.getX() <= maxX
                    && feet.getZ() >= minZ && feet.getZ() <= maxZ;
        }
    }

    private final List<ArmorStand> floatingTextStands = new ArrayList<>();

    private static final BlockPos V1_START = new BlockPos(0, 65, 0);
    private static final FinishPad V1_FINISH = new FinishPad(-1, 1, 67, 41, 43);
    private static final BlockPos V2_START = new BlockPos(0, 65, 0);
    private static final FinishPad V2_FINISH = new FinishPad(-2, 2, 70, 46, 50);
    private static final BlockPos V2_CHECKPOINT_PLATE = new BlockPos(0, 67, 23);
    private static final BlockPos V2_CHECKPOINT_RESPAWN = new BlockPos(0, 67, 22);
    private static final BlockPos V3_START = new BlockPos(0, 65, 0);
    private static final FinishPad V3_FINISH = new FinishPad(0, 4, 70, 51, 55);
    private static final BlockPos V3_CHECKPOINT_PLATE = new BlockPos(-3, 67, 24);
    private static final BlockPos V3_CHECKPOINT_RESPAWN = new BlockPos(-3, 67, 23);

    private boolean courseBuilt = false;
    private int builtVersion = 0;

    public int getArenaVersion() {
        return courseBuilt ? builtVersion : getConfiguredArenaVersion();
    }

    private int getConfiguredArenaVersion() {
        return Math.max(1, Math.min(3, Config.PARKOUR_ARENA_VERSION.get()));
    }

    public BlockPos getStartPosition() {
        return switch (getArenaVersion()) {
            case 1 -> V1_START;
            case 3 -> V3_START;
            default -> V2_START;
        };
    }

    /**
     * Landing anywhere on the finish pad completes the run; the pressure plate
     * only marks the middle. Requiring the exact plate made players who stuck
     * the final jump still lose seconds shuffling onto one block.
     */
    public boolean isOnFinishPad(BlockPos feet) {
        FinishPad pad = switch (getArenaVersion()) {
            case 1 -> V1_FINISH;
            case 3 -> V3_FINISH;
            default -> V2_FINISH;
        };
        return pad.contains(feet);
    }

    public boolean hasCheckpoint() {
        return getArenaVersion() >= 2;
    }

    public boolean isInCheckpointArea(BlockPos pos) {
        if (pos.getY() != 67) return false;
        if (getArenaVersion() == 2) {
            return pos.getX() >= -1 && pos.getX() <= 1
                    && pos.getZ() >= 22 && pos.getZ() <= 24;
        }
        if (getArenaVersion() == 3) {
            return pos.getX() >= -4 && pos.getX() <= -2
                    && pos.getZ() >= 23 && pos.getZ() <= 25;
        }
        return false;
    }

    public BlockPos getCheckpointRespawnPosition() {
        return getArenaVersion() == 3 ? V3_CHECKPOINT_RESPAWN : V2_CHECKPOINT_RESPAWN;
    }

    public AABB getBounds() {
        return new AABB(-12, 48, -8, 13, 96, 61);
    }

    public boolean needsRebuild() {
        return !courseBuilt || builtVersion != getConfiguredArenaVersion();
    }

    public void buildIfNeeded(ServerLevel parkourLevel) {
        if (!needsRebuild()) return;
        int version = getConfiguredArenaVersion();
        CreatiIntegration.LOGGER.info("Building parkour course version {}...", version);
        generateCourse(parkourLevel, version);
        courseBuilt = true;
        builtVersion = version;
        CreatiIntegration.LOGGER.info("Parkour course version {} built!", version);
    }

    private void generateCourse(ServerLevel level, int version) {
        clearFloatingText(level);

        // Clear the full course envelope so layout changes never leave stale
        // platforms, frames, or water behind when switching versions.
        for (int x = -12; x <= 12; x++) {
            for (int z = -8; z <= 60; z++) {
                for (int y = 48; y <= 78; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // The block pass synchronously loads every course chunk. Clear text a
        // second time now that markers saved near old checkpoints and finish
        // platforms are guaranteed to be loaded and visible to the query.
        clearFloatingText(level);

        switch (version) {
            case 1 -> buildLegacyCourse(level);
            case 3 -> buildPrismRelay(level);
            default -> buildFoundrySprint(level);
        }
        spawnVersionLabels(level, version);
    }

    private void buildLegacyCourse(ServerLevel level) {

        // 1. Start platform (3x3 gold)
        buildPlatform(level, 0, 64, 0, 3, Blocks.GOLD_BLOCK);

        // 2. Flat warmup gap (1x1 stone)
        buildPlatform(level, 0, 64, 3, 1, Blocks.STONE);

        // 3. Lateral movement (1x1 stone)
        buildPlatform(level, 1, 64, 6, 1, Blocks.STONE);

        // 4. Step up +1 (1x1 andesite)
        buildPlatform(level, 0, 65, 9, 1, Blocks.POLISHED_ANDESITE);

        // 5. Slime block bounce (1x1) — landing on slime bounces the player;
        //    they must control the bounce to reach the next platform.
        buildPlatform(level, 0, 65, 13, 1, Blocks.SLIME_BLOCK);

        // 6. Landing after slime bounce (1x1 stone)
        buildPlatform(level, 0, 65, 16, 1, Blocks.STONE);

        // 7. Packed ice slide platform (3x3) — the player must control their
        //    momentum to avoid sliding off the edge.
        buildPlatform(level, 0, 65, 19, 3, Blocks.PACKED_ICE);

        // 8. Step up +1 from ice (1x1 cobblestone)
        buildPlatform(level, 0, 66, 22, 1, Blocks.COBBLESTONE);

        // 9. Ladder climb segment.
        //    Cobblestone pillar (support) with ladders on the -Z face.
        int ladderZ = 25;
        for (int dy = 1; dy <= 3; dy++) {
            setBlock(level, 0, 66 + dy, ladderZ, Blocks.COBBLESTONE);
            level.setBlockAndUpdate(new BlockPos(0, 66 + dy, ladderZ - 1),
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        // Top of the pillar — landing before the next jump.
        buildPlatform(level, 0, 70, 25, 1, Blocks.STONE_BRICKS);

        // 10. Jump from ladder top (1x1 granite)
        buildPlatform(level, 0, 70, 28, 1, Blocks.POLISHED_GRANITE);

        // 11. Step down -1 (1x1 smooth stone)
        buildPlatform(level, 0, 69, 31, 1, Blocks.SMOOTH_STONE);

        // 12. Slime block bounce landing (1x1) — the player bounces on landing,
        //     making the takeoff for the next jump require timing.
        buildPlatform(level, 0, 69, 34, 1, Blocks.SLIME_BLOCK);

        // 13. Jump from honey — harder because honey slows momentum.
        buildPlatform(level, 0, 69, 37, 1, Blocks.HONEY_BLOCK);

        // 14. Water hazard gap (1x1 iron) — a 3-block gap over a water pit.
        //     Falling into the water resets the player to the start.
        buildPlatform(level, 0, 69, 40, 1, Blocks.IRON_BLOCK);
        // Water pit below the gap (visual hazard + early-reset trigger). It is
        // placed without block updates on a solid bed so it can't flow away
        // into the void below the course.
        for (int dx = -1; dx <= 1; dx++) {
            for (int z = 38; z <= 39; z++) {
                level.setBlock(new BlockPos(dx, 62, z), Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(dx, 63, z), Blocks.WATER.defaultBlockState(), 2);
            }
        }

        // 15. End platform (3x3 diamond) with pressure plate — step down -3.
        buildPlatform(level, 0, 66, 42, 3, Blocks.DIAMOND_BLOCK);
        level.setBlockAndUpdate(new BlockPos(0, 67, 42), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

    }

    /**
     * Version 2: a compact industrial sprint with readable jumps, one narrow
     * balance section, a midpoint checkpoint, an ascending diagonal, and a
     * momentum finish. A water basin returns misses quickly so a paid Twitch
     * interaction stays tense without becoming a long punishment.
     */
    private void buildFoundrySprint(ServerLevel level) {
        buildResetBasin(level);

        for (int frameZ : new int[]{-3, 13, 27, 41, 54}) {
            buildFoundryFrame(level, frameZ);
        }

        // Start deck: enough room to orient, but the route immediately narrows.
        buildRect(level, -2, 2, 64, -2, 2, Blocks.POLISHED_BLACKSTONE_BRICKS);
        buildRect(level, -1, 1, 64, -1, 1, Blocks.GOLD_BLOCK);
        setBlock(level, 0, 64, 2, Blocks.LIME_CONCRETE);

        // First half: diagonal changes and narrow landings demand deliberate
        // movement without turning the route into frame-perfect jumps.
        buildRect(level, 0, 1, 64, 5, 6, Blocks.CUT_COPPER);
        buildRect(level, -2, -2, 65, 9, 10, Blocks.EXPOSED_CUT_COPPER);
        buildRect(level, -3, -3, 65, 13, 15, Blocks.WEATHERED_CUT_COPPER);
        buildRect(level, -1, -1, 66, 18, 19, Blocks.OXIDIZED_CUT_COPPER);

        // Midpoint checkpoint. Missing after this point returns here rather
        // than invalidating the entire run.
        buildRect(level, -1, 1, 66, 22, 24, Blocks.POLISHED_BLACKSTONE_BRICKS);
        setBlock(level, 0, 66, 23, Blocks.GOLD_BLOCK);
        level.setBlockAndUpdate(V2_CHECKPOINT_PLATE,
                Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

        // Second half: compact 1x2 landings make the rising diagonal less
        // automatic while preserving a readable sprint rhythm.
        buildRect(level, 3, 3, 67, 27, 28, Blocks.CUT_COPPER);
        buildRect(level, 5, 6, 68, 31, 31, Blocks.EXPOSED_CUT_COPPER);
        // Kept within a 1x2 diagonal of the previous pad: the old position
        // asked for a three-block gap while climbing, the only near-maximum
        // jump on an otherwise rhythm-focused course.
        buildRect(level, 3, 3, 69, 34, 35, Blocks.WEATHERED_CUT_COPPER);

        // Ice supplies momentum for the final three-block gap. The finish is
        // 5x5, so the last jump has a generous landing instead of a tiny pad.
        buildRect(level, 0, 0, 69, 38, 42, Blocks.PACKED_ICE);
        setBlock(level, 0, 69, 42, Blocks.LIME_CONCRETE);
        buildRect(level, -2, 2, 69, 46, 50, Blocks.DIAMOND_BLOCK);
        buildRect(level, -1, 1, 69, 47, 49, Blocks.EMERALD_BLOCK);
        level.setBlockAndUpdate(new BlockPos(0, 70, 48),
                Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

    }

    /**
     * Version 3: a harder zig-zag course through bright arcane ruins. The
     * route alternates thin 1x2 pads, height changes, and diagonal jumps so it
     * feels distinct from the mostly forward Foundry Sprint. One checkpoint
     * keeps the Twitch interaction quick even when the player misses late.
     */
    private void buildPrismRelay(ServerLevel level) {
        buildRelayBasin(level);

        for (int archZ : new int[]{-3, 10, 24, 38, 56}) {
            buildRelayArch(level, archZ);
        }

        // Broad launch deck, followed by an alternating series of increasingly
        // precise landings. The cyan block clearly identifies the first jump.
        buildRect(level, -2, 2, 64, -2, 2, Blocks.QUARTZ_BRICKS);
        buildRect(level, -1, 1, 64, -1, 1, Blocks.AMETHYST_BLOCK);
        setBlock(level, 0, 64, 2, Blocks.CYAN_CONCRETE);

        buildRect(level, -3, -2, 64, 5, 6, Blocks.PURPUR_BLOCK);
        buildRect(level, 0, 0, 65, 9, 10, Blocks.CHISELED_QUARTZ_BLOCK);
        buildRect(level, 3, 4, 65, 13, 13, Blocks.AMETHYST_BLOCK);
        buildRect(level, 1, 1, 65, 16, 17, Blocks.PURPUR_BLOCK);
        buildRect(level, -2, -1, 66, 20, 20, Blocks.CHISELED_QUARTZ_BLOCK);

        // The relay pad is large enough to stabilize, but the route immediately
        // returns to narrow alternating jumps afterward.
        buildRect(level, -4, -2, 66, 23, 25, Blocks.QUARTZ_BRICKS);
        setBlock(level, -3, 66, 24, Blocks.GOLD_BLOCK);
        level.setBlockAndUpdate(V3_CHECKPOINT_PLATE,
                Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

        buildRect(level, 0, 1, 67, 28, 29, Blocks.AMETHYST_BLOCK);
        buildRect(level, 3, 3, 68, 32, 33, Blocks.PURPUR_BLOCK);
        buildRect(level, 0, 0, 68, 36, 37, Blocks.CHISELED_QUARTZ_BLOCK);
        buildRect(level, -3, -2, 69, 40, 40, Blocks.AMETHYST_BLOCK);
        buildRect(level, 0, 0, 69, 43, 44, Blocks.PURPUR_BLOCK);
        buildRect(level, 3, 4, 69, 47, 47, Blocks.CHISELED_QUARTZ_BLOCK);

        // A three-block final gap with a broad landing, so success is about
        // carrying momentum rather than hitting one exact pixel.
        buildRect(level, 0, 4, 69, 51, 55, Blocks.QUARTZ_BRICKS);
        buildRect(level, 1, 3, 69, 52, 54, Blocks.DIAMOND_BLOCK);
        setBlock(level, 2, 69, 53, Blocks.EMERALD_BLOCK);
        level.setBlockAndUpdate(new BlockPos(2, 70, 53),
                Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());

    }

    private void buildResetBasin(ServerLevel level) {
        int minX = -10, maxX = 10;
        int minZ = -4, maxZ = 54;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ;
                level.setBlock(new BlockPos(x, 50, z), Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
                if (boundary) {
                    for (int y = 51; y <= 54; y++) {
                        Block wallBlock = (y == 54 && Math.floorMod(x + z, 6) == 0)
                                ? Blocks.SEA_LANTERN
                                : Blocks.PRISMARINE_BRICKS;
                        level.setBlock(new BlockPos(x, y, z), wallBlock.defaultBlockState(), 2);
                    }
                } else {
                    level.setBlock(new BlockPos(x, 51, z), Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
    }

    private void buildFoundryFrame(ServerLevel level, int z) {
        for (int y = 55; y <= 74; y++) {
            Block columnBlock = y % 5 == 0 ? Blocks.COPPER_BLOCK : Blocks.DEEPSLATE_BRICKS;
            setBlock(level, -10, y, z, columnBlock);
            setBlock(level, 10, y, z, columnBlock);
        }
        for (int x = -10; x <= 10; x++) {
            Block beamBlock = (x == -6 || x == 0 || x == 6)
                    ? Blocks.SEA_LANTERN
                    : Blocks.POLISHED_BLACKSTONE_BRICKS;
            setBlock(level, x, 74, z, beamBlock);
        }
    }

    private void buildRelayBasin(ServerLevel level) {
        int minX = -10, maxX = 10;
        int minZ = -4, maxZ = 58;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ;
                level.setBlock(new BlockPos(x, 50, z), Blocks.QUARTZ_BRICKS.defaultBlockState(), 2);
                if (boundary) {
                    for (int y = 51; y <= 54; y++) {
                        Block wallBlock = (y == 54 && Math.floorMod(x + z, 5) == 0)
                                ? Blocks.SEA_LANTERN
                                : Blocks.PURPUR_BLOCK;
                        level.setBlock(new BlockPos(x, y, z), wallBlock.defaultBlockState(), 2);
                    }
                } else {
                    level.setBlock(new BlockPos(x, 51, z), Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
    }

    private void buildRelayArch(ServerLevel level, int z) {
        for (int y = 55; y <= 75; y++) {
            Block columnBlock = y % 4 == 0 ? Blocks.AMETHYST_BLOCK : Blocks.QUARTZ_PILLAR;
            setBlock(level, -9, y, z, columnBlock);
            setBlock(level, 9, y, z, columnBlock);
        }
        for (int x = -9; x <= 9; x++) {
            Block beamBlock = Math.floorMod(x, 4) == 0
                    ? Blocks.SEA_LANTERN
                    : Blocks.SMOOTH_QUARTZ;
            setBlock(level, x, 75, z, beamBlock);
        }
    }

    private List<FloatingLabel> getVersionLabels(int version) {
        return switch (version) {
            case 1 -> List.of(
                    new FloatingLabel(new BlockPos(0, 68, 0),
                            Component.literal("\u00A7a\u00A7lStart").withStyle(style -> style.withBold(true))),
                    new FloatingLabel(new BlockPos(0, 69, 42),
                            Component.literal("\u00A7b\u00A7lFinish!").withStyle(style -> style.withBold(true)))
            );
            case 3 -> List.of(
                    new FloatingLabel(new BlockPos(0, 68, 0),
                            Component.literal("\u00A7d\u00A7lPrism Relay \u00A77[V3]")
                                    .withStyle(style -> style.withBold(true))),
                    new FloatingLabel(new BlockPos(0, 67, 0),
                            Component.literal("\u00A77Commit to the diagonals")),
                    new FloatingLabel(new BlockPos(-3, 70, 24),
                            Component.literal("\u00A7e\u00A7lRelay Checkpoint")),
                    new FloatingLabel(new BlockPos(2, 73, 53),
                            Component.literal("\u00A7b\u00A7lFinish!").withStyle(style -> style.withBold(true)))
            );
            default -> List.of(
                    new FloatingLabel(new BlockPos(0, 68, 0),
                            Component.literal("\u00A76\u00A7lFoundry Sprint \u00A77[V2]")
                                    .withStyle(style -> style.withBold(true))),
                    new FloatingLabel(new BlockPos(0, 67, 0),
                            Component.literal("\u00A77Keep your momentum")),
                    new FloatingLabel(new BlockPos(0, 70, 23),
                            Component.literal("\u00A7e\u00A7lCheckpoint")),
                    new FloatingLabel(new BlockPos(0, 73, 48),
                            Component.literal("\u00A7b\u00A7lFinish!").withStyle(style -> style.withBold(true)))
            );
        };
    }

    private void spawnVersionLabels(ServerLevel level, int version) {
        for (FloatingLabel label : getVersionLabels(version)) {
            spawnFloatingText(level, label.pos(), label.text());
        }
    }

    /**
     * Repairs labels as their chunks load during a run. Only one exact copy of
     * each label belonging to the built arena version survives; markers from
     * older layouts and duplicate saved entities are discarded.
     */
    public void reconcileFloatingText(ServerLevel level) {
        List<FloatingLabel> desired = getVersionLabels(getArenaVersion());
        boolean[] found = new boolean[desired.size()];

        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, getBounds())) {
            Component name = stand.getCustomName();
            if (!stand.isInvisible() || name == null) continue;

            int match = -1;
            for (int i = 0; i < desired.size(); i++) {
                FloatingLabel label = desired.get(i);
                if (stand.blockPosition().equals(label.pos())
                        && name.getString().equals(label.text().getString())) {
                    match = i;
                    break;
                }
            }

            if (match < 0 || found[match]) {
                stand.discard();
                floatingTextStands.remove(stand);
            } else {
                found[match] = true;
                stand.addTag(TEXT_MARKER_TAG);
                if (!floatingTextStands.contains(stand)) {
                    floatingTextStands.add(stand);
                }
            }
        }

        floatingTextStands.removeIf(ArmorStand::isRemoved);
        for (int i = 0; i < desired.size(); i++) {
            FloatingLabel label = desired.get(i);
            if (!found[i] && level.hasChunkAt(label.pos())) {
                spawnFloatingText(level, label.pos(), label.text());
            }
        }
    }

    private void clearFloatingText(ServerLevel level) {
        for (ArmorStand stand : floatingTextStands) {
            stand.discard();
        }
        floatingTextStands.clear();
        AABB box = getBounds();
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box)) {
            stand.discard();
        }
    }

    private void spawnFloatingText(ServerLevel level, BlockPos pos, Component text) {
        // Rebuilds can happen before discarded entities have finished syncing
        // to clients. Remove any marker already occupying this label position
        // so repeated rebuilds never produce stacked text.
        AABB markerBox = new AABB(
                pos.getX() + 0.25, pos.getY() - 0.25, pos.getZ() + 0.25,
                pos.getX() + 0.75, pos.getY() + 0.25, pos.getZ() + 0.75);
        for (ArmorStand existing : level.getEntitiesOfClass(ArmorStand.class, markerBox)) {
            if (existing.isInvisible() && existing.getCustomName() != null) {
                existing.discard();
            }
        }

        ArmorStand armorStand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        armorStand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte) (armorStand.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS) | 0x10));
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);
        armorStand.addTag(TEXT_MARKER_TAG);
        level.addFreshEntity(armorStand);
        floatingTextStands.add(armorStand);
    }

    private void setBlock(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), block.defaultBlockState());
    }

    private void buildRect(ServerLevel level, int minX, int maxX, int y,
                           int minZ, int maxZ, Block block) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                setBlock(level, x, y, z, block);
            }
        }
    }

    /**
     * Builds a size x size platform centered on (centerX, centerZ) at height y.
     * size=1 -> 1x1, size=2 -> 2x2, size=3 -> 3x3 (correctly centered).
     */
    private void buildPlatform(ServerLevel level, int centerX, int y, int centerZ, int size, Block block) {
        int offset = (size - 1) / 2;
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                level.setBlockAndUpdate(new BlockPos(centerX - offset + dx, y, centerZ - offset + dz), block.defaultBlockState());
            }
        }
    }
}
