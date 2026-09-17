package com.baiqian.ifchat;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

/**
 * 聊天文本参数。既接受原版那种带引号的写法，也接受不带引号的中文。
 *
 * <p>原版 {@code StringArgumentType.string()} 读非引号内容时只认 {@code [0-9A-Za-z_.+-]}，
 * 中文会让它读成空串，于是 `execute if chat @a contains 开门` 直接报
 * {@code Expected whitespace to end one argument, but found trailing data}，
 * 必须写成 `contains "开门"`。对中文用户很反直觉。
 *
 * <p>规则：
 * <ul>
 *   <li>以 {@code "} 或 {@code '} 开头 → 交给 Brigadier 的 {@code readQuotedString()}，
 *       行为和原版完全一致（引号里可以带空格、转义）</li>
 *   <li>否则 → 一直读到空白为止，<b>允许任意非空白字符</b>，中文、日文、emoji 都行</li>
 * </ul>
 *
 * <p>因为遇到空格就停，后面照样能接 {@code run ...}，不会把后续命令吞掉。
 * 代价是文本本身含空格时仍然必须加引号——和原版语义一致。
 */
public class ChatTextArgument implements ArgumentType<String> {

    private static final Collection<String> EXAMPLES = List.of("开门", "\"开门\"", "\"hello world\"");

    public static ChatTextArgument chatText() {
        return new ChatTextArgument();
    }

    public static String getText(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
            return reader.readQuotedString();
        }

        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
