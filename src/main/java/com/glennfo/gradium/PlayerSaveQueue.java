package com.glennfo.gradium;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerList;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerSaveQueue {

    public static class Entry {
        public final PlayerList list;
        public final ServerPlayerEntity player;
        Entry(PlayerList list, ServerPlayerEntity player) {
            this.list = list;
            this.player = player;
        }
    }

    public static volatile boolean processing = false;

    private static final Deque<Entry> queue = new ArrayDeque<Entry>();
    private static final Set<UUID> queued = new HashSet<UUID>();

    private static volatile Method saveMethod = null;
    private static volatile boolean saveMethodResolved = false;

    private static Method resolveSaveMethod() {
        if (saveMethodResolved) return saveMethod;
        synchronized (PlayerSaveQueue.class) {
            if (saveMethodResolved) return saveMethod;
            for (String name : new String[]{"save", "func_72364_a"}) {
                try {
                    Method m = PlayerList.class.getDeclaredMethod(name, ServerPlayerEntity.class);
                    m.setAccessible(true);
                    saveMethod = m;
                    saveMethodResolved = true;
                    return saveMethod;
                } catch (NoSuchMethodException ignored) {}
            }
            for (Method m : PlayerList.class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0] == ServerPlayerEntity.class
                        && m.getReturnType() == void.class) {
                    try {
                        m.setAccessible(true);
                        saveMethod = m;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            if (saveMethod == null) {
                System.err.println("[Gradium] PlayerSaveQueue: could not resolve PlayerList.save(ServerPlayerEntity)");
            }
            saveMethodResolved = true;
            return saveMethod;
        }
    }

    public static synchronized boolean enqueue(PlayerList list, ServerPlayerEntity player) {
        UUID uuid = player.getUUID();
        if (!queued.add(uuid)) return false;
        queue.addLast(new Entry(list, player));
        return true;
    }

    public static synchronized void tick(int n) {
        if (queue.isEmpty()) return;
        processing = true;
        try {
            int remaining = n;
            while (remaining > 0 && !queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(entry.player.getUUID());
                savePlayer(entry);
                remaining--;
            }
        } finally {
            processing = false;
        }
    }

    public static synchronized void drainAll() {
        processing = true;
        try {
            while (!queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(entry.player.getUUID());
                savePlayer(entry);
            }
        } finally {
            processing = false;
            queue.clear();
            queued.clear();
        }
    }

    public static synchronized void clear() {
        queue.clear();
        queued.clear();
    }

    public static synchronized int size() {
        return queue.size();
    }

    private static void savePlayer(Entry entry) {
        Method m = resolveSaveMethod();
        if (m == null) {
            System.err.println("[Gradium] PlayerSaveQueue: no save method — skipping player: "
                    + entry.player.getGameProfile().getName());
            return;
        }
        try {
            m.invoke(entry.list, entry.player);
        } catch (Exception e) {
            System.err.println("[Gradium] PlayerSaveQueue: save failed for '"
                    + entry.player.getGameProfile().getName() + "': " + e.getMessage());
        }
    }
}
