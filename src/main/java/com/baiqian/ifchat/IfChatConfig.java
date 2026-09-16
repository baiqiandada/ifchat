package com.baiqian.ifchat;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 通用配置。访问器在配置尚未加载时会退回内置默认值，避免命令/mixin 提前执行时抛异常。
 */
public final class IfChatConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue CHAT_WINDOW_TICKS;
    public static final ForgeConfigSpec.BooleanValue CONSUME_ON_MATCH;
    public static final ForgeConfigSpec.BooleanValue FORCE_UNSIGNED_CHAT;

    private static final int DEFAULT_WINDOW_TICKS = 100;
    private static final boolean DEFAULT_CONSUME_ON_MATCH = true;
    private static final boolean DEFAULT_FORCE_UNSIGNED_CHAT = true;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "execute if chat / execute unless chat 分支的设置。",
                "Settings for the 'execute if chat' / 'execute unless chat' branch."
        ).push("chat");

        CHAT_WINDOW_TICKS = builder
                .comment(
                        "一条聊天记录的有效期，单位是游戏刻（20 刻 = 1 秒）。",
                        "超过这个时间之后，这条发言不再被 execute if chat 命中。",
                        "100 刻 = 5 秒；设为 0 表示只在发言的当刻有效。",
                        "How long a chat message stays detectable, in game ticks (20 ticks = 1 second)."
                )
                .defineInRange("chatWindowTicks", DEFAULT_WINDOW_TICKS, 0, 72000);

        CONSUME_ON_MATCH = builder
                .comment(
                        "匹配成功后是否立刻清除这条聊天记录。",
                        "开启（默认）：循环命令方块只会触发一次，不会被同一句话反复触发。",
                        "关闭：在有效期内每一刻都会判定成功，方便同时被多个条件读取。",
                        "If true, a matching chat record is consumed immediately so that a repeating",
                        "command block fires only once. If false, the condition stays true until the",
                        "window expires, so several conditions can all read the same message."
                )
                .define("consumeOnMatch", DEFAULT_CONSUME_ON_MATCH);

        builder.pop();

        builder.comment(
                "聊天签名设置：用于规避 1.20.1 的『聊天消息验证失败』掉线。",
                "Chat signing settings, used to work around the 1.20.1 'chat validation failed' kick."
        ).push("chatSigning");

        FORCE_UNSIGNED_CHAT = builder
                .comment(
                        "是否强制把服务端聊天改成未签名广播。",
                        "开启（默认）：聊天以未签名形式下发，客户端不做签名校验，",
                        "          可避免『某玩家一说话，另一个客户端就掉线并提示聊天消息验证失败』。",
                        "关闭：恢复原版签名聊天（所有客户端都能正常访问 Mojang 验证服务器时可以关掉）。",
                        "If true, server chat is broadcast unsigned so clients skip signature validation."
                )
                .define("forceUnsignedChat", DEFAULT_FORCE_UNSIGNED_CHAT);

        builder.pop();

        SPEC = builder.build();
    }

    private IfChatConfig() {
    }

    public static int windowTicks() {
        return SPEC.isLoaded() ? CHAT_WINDOW_TICKS.get() : DEFAULT_WINDOW_TICKS;
    }

    public static boolean consumeOnMatch() {
        return !SPEC.isLoaded() || CONSUME_ON_MATCH.get();
    }

    public static boolean forceUnsignedChat() {
        return !SPEC.isLoaded() || FORCE_UNSIGNED_CHAT.get();
    }
}
