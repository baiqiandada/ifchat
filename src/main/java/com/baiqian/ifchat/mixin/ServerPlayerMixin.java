package com.baiqian.ifchat.mixin;

import com.baiqian.ifchat.IfChat;
import com.baiqian.ifchat.IfChatConfig;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 把"发给某个玩家的聊天"从签名形式改成未签名形式，用于规避 1.20.1 的
 * "聊天消息验证失败"掉线（详见 {@link IfChat} 的说明）。
 *
 * <p>{@code ServerPlayer#sendChatMessage} 是服务端把聊天投递给单个玩家的<b>唯一收口</b>：
 * <ul>
 *   <li>{@code PlayerList#broadcastChatMessage}（普通聊天、{@code /say}）</li>
 *   <li>{@code MsgCommand}（{@code /msg}、{@code /tell}、{@code /w}）</li>
 *   <li>{@code CommandSourceStack#sendChatMessage}（命令源发起的聊天）</li>
 * </ul>
 * 都会走到这里，所以只需挂这一处。
 *
 * <p>{@code OutgoingChatMessage} 决定下发哪种数据包：
 * <pre>
 *   OutgoingChatMessage.Player    -> ClientboundPlayerChatPacket    （客户端会校验签名）
 *   OutgoingChatMessage.Disguised -> ClientboundDisguisedChatPacket （客户端不校验）
 * </pre>
 * 把前者替换成后者，客户端就不会再因为验不过签名而断开连接。
 * 显示效果不变：{@code ChatType.Bound} 里带着发送者名字，渲染出来仍是 {@code <玩家名> 消息}。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @ModifyVariable(
            method = "sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private OutgoingChatMessage ifchat$forceUnsigned(OutgoingChatMessage message) {
        if (!IfChatConfig.forceUnsignedChat()) {
            return message;
        }
        if (!(message instanceof OutgoingChatMessage.Player playerMessage)) {
            return message;
        }
        IfChat.logFirstChatStrip();
        return new OutgoingChatMessage.Disguised(playerMessage.message().decoratedContent());
    }
}
