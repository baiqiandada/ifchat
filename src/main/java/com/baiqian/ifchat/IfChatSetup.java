package com.baiqian.ifchat;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 注册自定义参数类型。
 *
 * <p><b>两步缺一不可，只做其中一步都是错的：</b>
 *
 * <ol>
 *   <li><b>进注册表</b>（{@link DeferredRegister}）：服务端登录时要把整棵命令树打包发给客户端做
 *       Tab 补全，写入端用的是
 *       {@code BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(info)}。
 *       没进注册表的话 {@code getId} 返回 -1，客户端 {@code byId(-1)} 得 null，
 *       那个参数节点会被直接丢弃 —— 表现是<b>客户端 Tab 补全里这一支不见了</b>
 *       （命令本身还能用，因为服务端是拿原始字符串自己解析的，所以不容易发现）。</li>
 *   <li><b>填 BY_CLASS</b>（{@link ArgumentTypeInfos#registerByClass}）：写入端构造
 *       {@code ArgumentNodeStub} 时走的是 {@code ArgumentTypeInfos.unpack(type)}，
 *       查的是这个静态 map。</li>
 * </ol>
 *
 * <p>Forge 自己注册 {@code forge:modid} / {@code forge:enum} 就是这两步一起做的，
 * 见 {@code ForgeMod} 里的 {@code COMMAND_ARGUMENT_TYPES}。
 */
public final class IfChatSetup {

    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, IfChat.MODID);

    /** 注册 id 为 {@code ifchat:chat_text}。 */
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> CHAT_TEXT =
            ARGUMENT_TYPES.register("chat_text", () -> ArgumentTypeInfos.registerByClass(
                    ChatTextArgument.class,
                    SingletonArgumentInfo.contextFree(ChatTextArgument::new)));

    public static void register(IEventBus modEventBus) {
        ARGUMENT_TYPES.register(modEventBus);
    }

    private IfChatSetup() {
    }
}
