# Relay

一个 Minecraft Fabric 模组，提供基于**双栈执行模型**的栈图编程系统。玩家可为工具、方块、实体编写自动化行为程序，实现图灵完备的计算能力。

执行模型深受 [Hex Casting](https://github.com/FallingColors/HexCasting) 启发。

## 核心概念

**双栈执行模型** —— 图灵机理论的最小工程实现：

```
while program_stack:
    operation = program_stack.pop()
    operation.execute(data_stack, program_stack)
```

- **数据栈**：存储操作数和中间结果
- **程序栈**：存储待执行的操作
- **统一接口**：所有操作（字面量、指令、列表）实现相同的 `execute` 方法
- **代码即数据**：列表可被 `eva` 动态展开执行

### 指令集

| 类别 | 指令 |
|------|------|
| 核心 | `copy` `eva` `if` `stop` |
| 算术 | `add` `sub` `mul` `div` |
| 逻辑 | `and` `or` `not` |
| 比较 | `eq` `lt` `gt` |
| 栈操作 | `dup` `pop` `swap` |
| 通信 | `send` `recv` `peek` |
| 列表 | `list-get` `list-set` `list-append` `list-length` |

### 游戏物品

| 物品 | 作用 |
|------|------|
| 运算核心 | 提供操作数预算，相邻合并增强算力 |
| 法术磁盘 | 存储栈图程序 |
| 能量模块 | 存储紫水晶能量 |
| 世界交互器 | 决定是否允许与世界交互 |
| 外壳 | 容器，决定形态（方块/实体/工具） |

## 项目结构

```
src/
├── main/java/qdream/relay/
│   ├── Relay.java                  # 模组入口
│   ├── engine/                     # 执行引擎
│   │   ├── Executable.java         # 可执行单元接口
│   │   └── StateMachine.java       # 双栈状态机
│   ├── core/                       # 核心系统
│   │   ├── CommunicationSystem.java
│   │   ├── CoreGroup.java
│   │   ├── EnergySystem.java
│   │   ├── Scheduler.java
│   │   └── ShellContainer.java
│   ├── types/                      # Iota 数据类型
│   ├── operations/                 # 操作实现
│   │   ├── arithmetic/             # 算术操作
│   │   ├── base/                   # 基础栈操作
│   │   ├── communication/          # 通信操作
│   │   ├── control/                # 控制流操作
│   │   ├── list/                   # 列表操作
│   │   └── logic/                  # 逻辑/比较操作
│   ├── mc/                         # Minecraft 层集成
│   │   ├── OperationRegistry.java
│   │   ├── base/                   # Data/Spell 基类
│   │   └── ProgramCompiler.java
│   ├── blocks/                     # 方块与方块实体
│   ├── entities/                   # 实体外壳
│   ├── items/                      # 物品
│   ├── screen/                     # GUI
│   └── networking/                 # 网络包
└── client/java/qdream/relay/client/
    ├── editor/                     # 栈图编辑器
    ├── screen/                     # 客户端 GUI
    └── networking/                 # 客户端网络
```

## 构建

需要 Java 25+。

```bash
# 克隆项目
git clone https://github.com/QiDream-hub/Relay.git
cd Relay

# 构建
./gradlew build

# 产物位于 build/libs/
```

## 环境要求

| 依赖 | 版本 |
|------|------|
| Minecraft | 26.1.2 |
| Fabric Loader | ≥ 0.19.2 |
| Fabric API | 0.150.0+26.1.2 |
| Java | ≥ 25 |

## 开发

```bash
# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer
```

## 许可证

CC0-1.0
