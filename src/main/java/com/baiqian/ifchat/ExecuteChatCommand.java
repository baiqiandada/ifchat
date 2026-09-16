package com.baiqian.ifchat;

import java.util.Collection;
import java.util.Collections;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * 往 {@code execute if} / {@code execute unless} 上挂一个 {@code chat} 条件分支。
 *
 * <p>语法：
 * <pre>
 *   execute if     chat &lt;targets&gt; exact    "文本"
 *   execute if     chat &lt;targets&gt; contains "文本"
 *   execute unless chat &lt;targets&gt; exact    "文本"
 *   execute unless chat &lt;targets&gt; contains "文本"
 * </pre>
 *
 * <p>实现方式与 vanilla 的 {@code ExecuteCommand.addConditionArguments} 完全一致：
 * 叶子节点用 {@code fork(execute根节点, 修饰器)} 把后续命令接到 execute 链上，
 * 同时用 {@code executes(...)} 处理"条件单独结尾"的用法（打印 Test passed / Test failed）。
 * 区别只是我们在命令树注册完成之后，用 Brigadier 公开的
 * {@link CommandNode#addChild(CommandNode)} 把节点挂上去，不需要 Mixin。
 */
public final class ExecuteChatCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String ARG_TARGETS = "targets";
    private static final String ARG_TEXT = "text";

    private static final SimpleCommandExceptionType FAIL =
            new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail"));

    private ExecuteChatCommand() {
    }

    /** 由 RegisterCommandsEvent 调用。 */
    public static void inject(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> executeNode = dispatcher.getRoot().getChild("execute");
        if (executeNode == null) {
            LOGGER.warn("[ifchat] 找不到 /execute 命令节点，execute if chat 分支未注入。");
            return;
        }
        injectBranch(executeNode, "if", true);
        injectBranch(executeNode, "unless", false);
    }

    private static void injectBranch(CommandNode<CommandSourceStack> executeNode, String branch, boolean isIf) {
        CommandNode<CommandSourceStack> conditionNode = executeNode.getChild(branch);
        if (conditionNode == null) {
            LOGGER.warn("[ifchat] 找不到 /execute {} 命令节点，该分支未注入。", branch);
            return;
        }

        LiteralCommandNode<CommandSourceStack> chatNode = Commands.literal("chat")
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                        .then(mode("exact", executeNode, isIf, true))
                        .then(mode("contains", executeNode, isIf, false)))
                .build();

        conditionNode.addChild(chatNode);
    }

    /**
     * 构造 {@code exact "文本"} / {@code contains "文本"} 这一层。
     *
     * @param exact true = 完全相等，false = 包含
     */
    private static ArgumentBuilder<CommandSourceStack, ?> mode(String literal,
                                                               CommandNode<CommandSourceStack> forkTarget,
                                                               boolean isIf,
                                                               boolean exact) {
        return Commands.literal(literal).then(
                Commands.argument(ARG_TEXT, StringArgumentType.string())
                        .fork(forkTarget, ctx -> forkResult(ctx, isIf, exact))
                        .executes(ctx -> {
                            if (test(ctx, exact) == isIf) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.translatable("commands.execute.conditional.pass"),
                                        false);
                                return 1;
                            }
                            throw FAIL.create();
                        }));
    }

    /** 条件成立时把当前 source 交回 execute 链，否则中断（返回空集合）。 */
    private static Collection<CommandSourceStack> forkResult(CommandContext<CommandSourceStack> ctx,
                                                             boolean isIf,
                                                             boolean exact) throws CommandSyntaxException {
        if (test(ctx, exact) == isIf) {
            return Collections.singleton(ctx.getSource());
        }
        return Collections.emptyList();
    }

    /** 只要 targets 里有任意一名玩家的最近发言命中，就算条件成立。 */
    private static boolean test(CommandContext<CommandSourceStack> ctx, boolean exact) throws CommandSyntaxException {
        String text = StringArgumentType.getString(ctx, ARG_TEXT);
        // 用 getOptionalPlayers：没有玩家时返回空集合，而不是抛 NO_PLAYERS_FOUND，
        // 与 vanilla 的 execute if entity 行为一致（条件不成立而不是命令报错）。
        Collection<ServerPlayer> players = EntityArgument.getOptionalPlayers(ctx, ARG_TARGETS);
        for (ServerPlayer player : players) {
            if (ChatTracker.matches(player.getUUID(), text, exact)) {
                return true;
            }
        }
        return false;
    }
}
