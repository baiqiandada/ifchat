package com.baiqian.ifchat;

import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 注册自定义参数类型。
 *
 * <p>这一步不能省：服务端登录时会把整棵命令树打包发给客户端做 Tab 补全
 * （{@code ClientboundCommandsPacket}），打包时要按注册表查参数类型对应的
 * {@code ArgumentTypeInfo}。没注册的话查出来是 null，发命令树时直接炸。
 *
 * <p>{@link ChatTextArgument} 没有任何配置项，所以用现成的
 * {@link SingletonArgumentInfo#contextFree} 就够了，不用自己写 ArgumentTypeInfo。
 */
@Mod.EventBusSubscriber(modid = IfChat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IfChatSetup {

    private IfChatSetup() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ArgumentTypeInfos.registerByClass(
                ChatTextArgument.class,
                SingletonArgumentInfo.contextFree(ChatTextArgument::new)));
    }
}
