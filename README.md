# IfChat

Minecraft 1.20.1 / Forge 47.x 模组，两件事：

- `execute if chat` —— 检测玩家最近说了什么
- 聊天强制未签名 —— 修联机时"某人说话某人掉线"的问题

## execute if chat

给原版 `execute` 挂了个 chat 条件：

```
execute if     chat <targets> exact    开门
execute if     chat <targets> contains 开门
execute unless chat <targets> exact    开门
execute unless chat <targets> contains 开门
```

`exact` 完全相等（分大小写），`contains` 包含就行。`<targets>` 里任意一个玩家命中即成立。

文本**可以直接写中文，不用加引号**。原版 `StringArgumentType` 读非引号内容时只认
`[0-9A-Za-z_.+-]`，中文会被它读成空串、直接报解析错误，所以这里自己实现了一个文本参数：
以引号开头就走原版逻辑，否则一直读到空白为止，中日文、emoji 都行。

文本里**含空格**时仍然要加引号：

```
# 有人说"开门"就开机关
execute if chat @a contains 开门 run setblock 10 64 10 stone

# 谁说了"你好"就给谁发提示
execute as @a if chat @a exact 你好 run tellraw @s "嗨"

# 文本带空格，加引号
execute if chat @a contains "开 门" run say 机关开了
```

不接 `run` 时和原版条件一样回显 `Test passed` / `Test failed`。

聊天记录默认保留 100 刻（5 秒），命中一次就清除。这是为了循环命令方块能用——
否则玩家说一句"开门"，命令方块会在 5 秒内每刻都触发一次。想让它在这段时间里一直为真，
把 `consumeOnMatch` 关掉。

## 聊天强制未签名

这条是绕过 1.20.1 的聊天签名机制，不是新功能。

1.20.1 的聊天消息带签名。有 Mojang 密钥的人发出的消息是签过名的，客户端收到必须验签，
验不过就主动断线并提示"聊天消息验证失败"。

如果某个客户端的网络拿不到 Mojang 的 Services 公钥（日志里是
`Ignoring chat session from ... due to missing Services public key`），
它就没法验证任何人的签名，于是别人一说话它就掉线——看起来就像"某人一开口某人掉线"。

这个模组把发给玩家的聊天由 `OutgoingChatMessage.Player` 换成 `Disguised`，改走
`ClientboundDisguisedChatPacket`，客户端对这条路径不验签，掉线随之消失。显示上没区别，
仍是 `<玩家名> 消息`，只是标记为未验证。

只有主机 / 服务端需要装，客机不用装。客机网络正常的话，把 `forceUnsignedChat`
关掉就恢复原版行为。

## 配置

`config/ifchat-common.toml`，首次启动生成：

```toml
[chat]
    # 聊天记录保留多久，单位游戏刻（20 刻 = 1 秒）。0 = 只在发言当刻有效
    chatWindowTicks = 100
    # 命中后是否立刻清除记录
    consumeOnMatch = true

[chatSigning]
    # 强制服务端聊天未签名下发
    forceUnsignedChat = true
```

## 编译

需要 JDK 17：

```
./gradlew clean build
```

产物在 `build/libs/`。

## 还没验证的

聊天未签名这条只在专用服务端上验证过，没试过两个真人客户端联机。有问题开 issue。
