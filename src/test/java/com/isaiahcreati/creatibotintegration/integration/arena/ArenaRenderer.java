package com.isaiahcreati.creatibotintegration.integration.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Draws a recorded arena as an isometric picture using block map colors. */
final class ArenaRenderer {

    private static final int TILE = 6;

    private ArenaRenderer() {}

    /**
     * Renders blocks matching the filter. Pass a filter that drops the half
     * nearest the camera to look inside enclosed arenas.
     */
    static void renderIso(MemoryCanvas canvas, Predicate<BlockPos> include, File out) throws IOException {
        List<Map.Entry<BlockPos, BlockState>> voxels = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : canvas.blocks.entrySet()) {
            if (include.test(e.getKey()) && colorOf(e.getValue()) != null) voxels.add(e);
        }
        if (voxels.isEmpty()) return;

        // Camera looks from +X/+Z down toward -X/-Z; nearer blocks are drawn last.
        voxels.sort(Comparator.<Map.Entry<BlockPos, BlockState>>comparingInt(e -> e.getKey().getX() + e.getKey().getZ())
                .thenComparingInt(e -> e.getKey().getY()));

        int minSx = Integer.MAX_VALUE, maxSx = Integer.MIN_VALUE, minSy = Integer.MAX_VALUE, maxSy = Integer.MIN_VALUE;
        for (Map.Entry<BlockPos, BlockState> e : voxels) {
            BlockPos p = e.getKey();
            int sx = sx(p), sy = sy(p);
            minSx = Math.min(minSx, sx - TILE); maxSx = Math.max(maxSx, sx + TILE);
            minSy = Math.min(minSy, sy - TILE); maxSy = Math.max(maxSy, sy + 2 * TILE);
        }
        int pad = 10;
        BufferedImage img = new BufferedImage(maxSx - minSx + 2 * pad, maxSy - minSy + 2 * pad, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(0x78A7FF));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        for (Map.Entry<BlockPos, BlockState> e : voxels) {
            BlockPos p = e.getKey();
            Color base = colorOf(e.getValue());
            int cx = sx(p) - minSx + pad;
            int cy = sy(p) - minSy + pad;
            int h = TILE / 2;
            // Top face.
            g.setColor(base);
            g.fillPolygon(new Polygon(new int[]{cx, cx + TILE, cx, cx - TILE}, new int[]{cy - h, cy, cy + h, cy}, 4));
            // +X face (right) and +Z face (left), shaded.
            g.setColor(shade(base, 0.8f));
            g.fillPolygon(new Polygon(new int[]{cx, cx + TILE, cx + TILE, cx}, new int[]{cy + h, cy, cy + TILE, cy + h + TILE}, 4));
            g.setColor(shade(base, 0.62f));
            g.fillPolygon(new Polygon(new int[]{cx - TILE, cx, cx, cx - TILE}, new int[]{cy, cy + h, cy + h + TILE, cy + TILE}, 4));
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    /** Top-down map: the highest visible block in each column, shaded by height. */
    static void renderTop(MemoryCanvas canvas, Predicate<BlockPos> include, File out) throws IOException {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos p : canvas.blocks.keySet()) {
            if (!include.test(p)) continue;
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
        }
        if (minX > maxX) return;
        int scale = 8;
        BufferedImage img = new BufferedImage((maxX - minX + 1) * scale, (maxZ - minZ + 1) * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Color c = new Color(0x78A7FF);
                for (int y = maxY; y >= minY; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!include.test(p)) continue;
                    Color bc = colorOf(canvas.get(x, y, z));
                    if (bc != null) {
                        float t = maxY == minY ? 1f : 0.55f + 0.45f * (y - minY) / (float) (maxY - minY);
                        c = shade(bc, t);
                        break;
                    }
                }
                g.setColor(c);
                g.fillRect((x - minX) * scale, (z - minZ) * scale, scale, scale);
            }
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    private static int sx(BlockPos p) { return (p.getX() - p.getZ()) * TILE; }
    private static int sy(BlockPos p) { return (p.getX() + p.getZ()) * TILE / 2 - p.getY() * TILE; }

    private static Color colorOf(BlockState state) {
        if (state.isAir() || state.is(Blocks.BARRIER)) return null;
        if (state.is(Blocks.GLASS)) return new Color(0xD8F0F8);
        if (state.is(Blocks.WATER)) return new Color(0x3F76E4);
        MapColor mc = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (mc == MapColor.NONE) return new Color(0x999999);
        return new Color(mc.col);
    }

    private static Color shade(Color c, float f) {
        return new Color(Math.min(255, (int) (c.getRed() * f)), Math.min(255, (int) (c.getGreen() * f)), Math.min(255, (int) (c.getBlue() * f)));
    }
}
