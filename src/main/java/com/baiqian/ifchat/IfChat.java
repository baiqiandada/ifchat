package com.baiqian.ifchat;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * IfChat —— 聊天相关的两件事，都在这一个模组里。
 *
 * <h2>1. {@code execute if chat} 条件分支</h2>
 * 给 {@code execute if} / {@code execute unless} 挂一个新的条件分支：
 * <pre>
 *   execute if     chat &lt;targets&gt; exact    "文本"
 *   execute if     chat &lt;targets&gt; contains "文本"
 *   execute unless chat &lt;targets&gt; exact    "文本"
 *   execute unless chat &lt;targets&gt; contains "文本"
 * </pre>
 * {@code exact} 完全相等，{@code contains} 包含；targets 里只要有一名玩家命中就算成立。
 * 见 {@code ExecuteChatCommand}。
 *
 * <h2>2. 聊天签名掉线的规避</h2>
 * 1.20.1 里，有 Mojang 密钥的一方发出的聊天是<b>签名</b>的，服务端以
 * {@code ClientboundPlayerChatPacket} 下发；客户端收到后必须用发送方的聊天会话密钥校验签名，
 * 校验不过就<b>自己断开连接</b>并提示"聊天消息验证失败"。
 *
 * <p>如果某个客户端因网络原因拿不到 Mojang 的 Services 公钥
 * （日志里表现为 {@code Ignoring chat session from ... due to missing Services public key}），
 * 它就永远验不过别人的签名消息 —— 于是"某个人一说话，另一个人就掉线"。
 *
 * <p>本模组把下发路径上的 {@code OutgoingChatMessage.Player} 换成
 * {@code OutgoingChatMessage.Disguised}，消息改走 {@code ClientboundDisguisedChatPacket}，
 * 客户端对这条路径<b>不做任何签名校验</b>，掉线问题随之消失。见
 * {@code mixin/ServerPlayerMixin}。
 *
 * <p>代价：聊天全部变成"未验证"，安全聊天功能失效；自建服 / 局域网环境可接受。
 * 不需要时把配置里的 {@code chatSigning.forceUnsignedChat} 改成 {@code false} 即可恢复原版行为。
 */
@Mod(IfChat.MODID)
public class IfChat {

    public static final String MODID = "ifchat";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicBoolean LOGGED_FIRST_CHAT_STRIP = new AtomicBoolean(false);

    public IfChat(FMLJavaModLoadingContext context) {
        IfChatSetup.register(context.getModEventBus());
        context.registerConfig(ModConfig.Type.COMMON, IfChatConfig.SPEC);
        LOGGER.info("[ifchat] 已加载：execute if chat 分支 + 聊天未签名下发。");
    }

    /** 只在第一次真正剥离签名时打一条日志，避免刷屏；同时也是"混入是否生效"的探针。 */
    public static void logFirstChatStrip() {
        if (LOGGED_FIRST_CHAT_STRIP.compareAndSet(false, true)) {
            LOGGER.info("[ifchat] 已把一条签名聊天改写为未签名广播（客户端不会再做签名校验）。");
        }
    }
}
