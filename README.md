# IfChat

> 一个 Minecraft Forge 1.20.1 模组，做两件和聊天有关的事。
> A Minecraft Forge 1.20.1 mod with two chat-related features.

- **环境**：Minecraft 1.20.1 + Forge 47.x
- **侧**：两端通用（`execute if chat` 跑在服务端；聊天未签名只需要主机/服务端装）

---

## 功能一：`execute if chat` 条件分支

给原版的 `execute if` / `execute unless` 增加一个 `chat` 条件，用来检测玩家最近的发言内容，
适合做"说暗号触发机关"这类地图玩法。

### 语法

```
execute if     chat <targets> exact    "文本"
execute if     chat <targets> contains "文本"
execute unless chat <targets> exact    "文本"
execute unless chat <targets> contains "文本"
```

- `exact` —— 完全相等（区分大小写）
- `contains` —— 包含即可
- `<targets>` 里只要有**任意一名**玩家命中，条件就算成立

### 示例

```mcfunction
# 有人说"开门"就打开机关
execute if chat @a contains "开门" run setblock 10 64 10 stone

# 精确匹配，并让命中的玩家本人收到提示
execute as @a if chat @a exact "你好" run tellraw @s "嗨"

# 只有没人说"安静"时才继续
execute unless chat @a contains "安静" run say 继续
```

单独使用时（不接 `run`）会像原版条件一样回显 `Test passed` / `Test failed`。

### 判定窗口与"消费"语义

聊天记录默认保留 **100 刻（5 秒）**，并且**命中一次就被清除**。

这是为了让循环命令方块能用：否则玩家说一句"开门"，循环命令方块会在有效期内每一刻都触发，
机关被反复打开。想改成"有效期内持续为真"，把 `consumeOnMatch` 关掉即可。

---

## 功能二：聊天强制未签名（规避联机掉线）

### 它解决什么问题

1.20.1 引入了**聊天签名**。有 Mojang 密钥的一方发出的聊天是带签名的，服务端以
`ClientboundPlayerChatPacket` 下发；客户端收到后必须用发送方的聊天会话密钥校验签名，
**校验不过就自己断开连接**，提示「聊天消息验证失败」。

如果某个客户端因为网络原因拿不到 Mojang 的 Services 公钥
（日志里表现为 `Ignoring chat session from ... due to missing Services public key`），
它就永远验不过别人的签名消息 —— 表现就是**"某个人一说话，另一个人就掉线"**。

### 怎么解决的

把下发路径上的 `OutgoingChatMessage.Player` 换成 `OutgoingChatMessage.Disguised`，
消息改走 `ClientboundDisguisedChatPacket`。客户端对这条路径**不做任何签名校验**，掉线随之消失。

显示效果不变：聊天类型里带着发送者名字，渲染出来仍然是 `<玩家名> 消息`，只是被标记为未验证。

> 注意：**只需要主机 / 服务端装**，客机不用装。
> 客机自己网络正常的话，把 `forceUnsignedChat` 关掉就能恢复原版签名聊天。

---

## 配置

首次启动后在 `config/ifchat-common.toml` 生成：

```toml
[chat]
    # 一条聊天记录的有效期，单位游戏刻（20 刻 = 1 秒）。0 表示只在发言的当刻有效。
    chatWindowTicks = 100
    # 匹配成功后是否立刻清除这条聊天记录。开启可避免循环命令方块重复触发。
    consumeOnMatch = true

[chatSigning]
    # 是否强制把服务端聊天改成未签名广播。
    forceUnsignedChat = true
```

---

## 构建

需要 JDK 17。

```bash
./gradlew clean build
```

产物：`build/libs/ifchat-1.0.0.jar`，丢进 `mods/` 即可。

---

## 实现说明

两个功能都是**纯服务端**逻辑，没有客户端渲染改动。

- **`execute if chat` 分支**：在 `RegisterCommandsEvent` 里对已经建好的命令树调用
  Brigadier 的 `CommandNode#addChild` 挂枝，**不需要 Mixin**。条件叶子照抄原版
  `ExecuteCommand` 的 `.fork(executeRoot, modifier)` + `.executes(...)` 组合。
- **聊天未签名**：一个 Mixin 挂在 `ServerPlayer#sendChatMessage` 上 —— 这是服务端把聊天
  投递给单个玩家的唯一收口（普通聊天、`/say`、`/msg`、`/teammsg` 全都汇到这里）。

---

## 已知限制

- 改成未签名后，聊天会显示为"未验证"，**安全聊天功能失效**。自建服 / 局域网场景可接受。
- 未签名下发这条路径做过专用服务端验证，**尚未在两个真实客户端联机场景下完整验证**。
- 原版在消息被"完全过滤"时会跳过发送，未签名路径总是发送。自建服 `FilterMask` 恒为
  `PASS_THROUGH`，实际无影响。
- 仅适配 1.20.1 / Forge 47.x。
