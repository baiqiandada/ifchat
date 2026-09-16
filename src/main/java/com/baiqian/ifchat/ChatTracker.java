package com.baiqian.ifchat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;

/**
 * 记录每个玩家最近一次发言，供 {@code execute if chat} 判定使用。
 *
 * <p>只保留"最近一条"，并且只在 {@link IfChatConfig#windowTicks()} 刻内有效；
 * 过期记录会在下次访问时顺手清掉，所以不会无限增长。
 */
public final class ChatTracker {

    private record Record(String text, long tick) {
    }

    private static final Map<UUID, Record> RECORDS = new ConcurrentHashMap<>();

    /** 内部时钟，由 ServerTickEvent 每刻推进一次（只在服务端线程写，读用 volatile 保证可见性）。 */
    private static volatile long currentTick;

    private ChatTracker() {
    }

    /** ServerTickEvent(END)：推进内部时钟。 */
    public static void advanceTick() {
        currentTick++;
    }

    /** ServerChatEvent：记下玩家刚发出的聊天原文。 */
    public static void recordChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String raw = event.getRawText();
        if (player == null || raw == null) {
            return;
        }
        record(player.getUUID(), raw);
    }

    /** 记录"某个玩家刚说了一句 text"。 */
    public static void record(UUID uuid, String text) {
        RECORDS.put(uuid, new Record(text, currentTick));
    }

    /**
     * 判断该玩家最近一条发言是否命中。
     *
     * @param uuid  玩家 UUID
     * @param text  要比较的文本
     * @param exact {@code true} = 完全相等，{@code false} = 包含
     */
    public static boolean matches(UUID uuid, String text, boolean exact) {
        Record record = RECORDS.get(uuid);
        if (record == null) {
            return false;
        }

        long age = currentTick - record.tick();
        if (age < 0L || age > IfChatConfig.windowTicks()) {
            RECORDS.remove(uuid, record);
            return false;
        }

        boolean hit = exact ? record.text().equals(text) : record.text().contains(text);
        if (hit && IfChatConfig.consumeOnMatch()) {
            RECORDS.remove(uuid, record);
        }
        return hit;
    }

    /** 清空全部记录（服务器关闭时调用）。 */
    public static void clear() {
        RECORDS.clear();
    }
}
