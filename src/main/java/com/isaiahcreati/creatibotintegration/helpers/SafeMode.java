package com.isaiahcreati.creatibotintegration.helpers;

import net.minecraft.server.MinecraftServer;

public class SafeMode {

    private static long safeModeUntilTick = Long.MIN_VALUE;
    private static MinecraftServer server;

    public static void setServer(MinecraftServer srv) {
        server = srv;
    }

    public static boolean isActive() {
        if (server == null) return false;
        return server.getTickCount() < safeModeUntilTick;
    }

    public static int getRemainingSeconds() {
        if (server == null || !isActive()) return 0;
        return (int) Math.max(0, (safeModeUntilTick - server.getTickCount()) / 20L);
    }

    public static void enable(MinecraftServer srv, int seconds) {
        server = srv;
        safeModeUntilTick = srv.getTickCount() + seconds * 20L;
    }

    public static void disable() {
        safeModeUntilTick = Long.MIN_VALUE;
    }
}