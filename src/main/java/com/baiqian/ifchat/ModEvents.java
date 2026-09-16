package com.baiqian.ifchat;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IfChat.MODID)
public final class ModEvents {

    private ModEvents() {
    }

    /** 命令树建好后，把 chat 分支挂到 execute 的 if / unless 上。 */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ExecuteChatCommand.inject(event.getDispatcher());
    }

    /** 玩家发言时记录原文。 */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ChatTracker.recordChat(event);
    }

    /** 推进内部时钟，用于判断聊天记录是否过期。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ChatTracker.advanceTick();
        }
    }

    /** 服务器关闭时清空记录，避免集成服务器重开世界后残留。 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ChatTracker.clear();
    }
}
