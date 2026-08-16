package com.glennfo.gradium;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Queues online players for deferred per-player save calls.
 *
 * <p>The mixin intercepts {@code PlayerList.saveAll()} and instead enqueues each online player
 * here. We drain {@code n} players per tick based on the adaptive rate. When {@code processing}
 * is true the mixin lets the real {@code saveAll()} through so we don't recurse.</p>
 *
 * <p>{@code PlayerList.save(ServerPlayer)} is {@code protected}, so we invoke it via reflection.
 * The method handle is resolved and cached once on first use.</p>
 */
public class PlayerSaveQueue {

    public record Entry(PlayerList list, ServerPlayer player) {}

    /** Set to true while we are deliberately saving players so the mixin passes through. */
    public static volatile boolean processing = false;

    private static final Deque<Entry> queue = new ArrayDeque<>();

    /** Dedup by player UUID. */
    private static final Set<UUID> queued = new HashSet<>();

    // Cached reflection handle for PlayerList.save(ServerPlayer) (protected).
    private static volatile Method saveMethod = null;
    private static volatile boolean saveMethodResolved = false;

    private static Method resolveSaveMethod() {
        if (saveMethodResolved) return saveMethod;
        synchronized (PlayerSaveQueue.class) {
            if (saveMethodResolved) return saveMethod;
            // Try known names first, then fall back to signature scan (handles obfuscated envs).
            for (String name : new String[]{"save", "m_11347_"}) {
                try {
                    Method m = PlayerList.class.getDeclaredMethod(name, ServerPlayer.class);
                    m.setAccessible(true);
                    saveMethod = m;
                    saveMethodResolved = true;
                    return saveMethod;
                } catch (NoSuchMethodException ignored) {}
            }
            // Signature scan: find any (ServerPlayer) -> void method on PlayerList.
            for (Method m : PlayerList.class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0] == ServerPlayer.class
                        && m.getReturnType() == void.class) {
                    try {
                        m.setAccessible(true);
                        saveMethod = m;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            if (saveMethod == null) {
                System.err.println("[Gradium] PlayerSaveQueue: could not resolve PlayerList.save(ServerPlayer)");
            }
            saveMethodResolved = true;
            return saveMethod;
        }
    }

    /** Enqueue a player for deferred saving. Dedup by UUID. */
    public static synchronized boolean enqueue(PlayerList list, ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!queued.add(uuid)) return false;
        queue.addLast(new Entry(list, player));
        return true;
    }

    /** Process up to {@code n} players from the front of the queue. Called once per server tick. */
    public static synchronized void tick(int n) {
        if (queue.isEmpty()) return;
        processing = true;
        try {
            int remaining = n;
            while (remaining > 0 && !queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(entry.player().getUUID());
                savePlayer(entry);
                remaining--;
            }
        } finally {
            processing = false;
        }
    }

    /** Immediately drain the entire queue. Called before level unload or server shutdown. */
    public static synchronized void drainAll() {
        processing = true;
        try {
            while (!queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(entry.player().getUUID());
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
                    + entry.player().getGameProfile().name());
            return;
        }
        try {
            m.invoke(entry.list(), entry.player());
        } catch (Exception e) {
            System.err.println("[Gradium] PlayerSaveQueue: save failed for '"
                    + entry.player().getGameProfile().name() + "': " + e.getMessage());
        }
    }
}
