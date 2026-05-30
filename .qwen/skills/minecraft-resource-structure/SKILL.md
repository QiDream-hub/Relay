---
name: minecraft-resource-structure
description: Minecraft 模组资源文件结构指南，包括纹理、模型、语言文件的正确位置和格式
source: auto-skill
extracted_at: '2026-05-30T20:10:00.000Z'
---

# Minecraft 模组资源文件结构指南

本文档描述 Minecraft 模组资源文件的正确目录结构、文件格式和常见问题排查方法。

## 标准目录结构

### Minecraft 26.1.2+ 完整结构

```
src/main/resources/assets/<modid>/
├── lang/
│   ├── zh_cn.json          # 简体中文语言文件
│   └── en_us.json          # 英文语言文件
├── textures/
│   ├── item/               # 物品纹理
│   │   └── <item_id>.png   # 16x16, 32x32, 64x64, 128x128, 或 256x256
│   └── block/              # 方块纹理
│       └── <block_id>.png
├── models/
│   ├── item/               # 物品模型
│   │   └── <item_id>.json
│   └── block/              # 方块模型
│       └── <block_id>.json
├── items/                  # 客户端物品定义 (26.1.2+ 必需)
│   └── <item_id>.json
└── blockstates/            # 方块状态定义
    └── <block_id>.json
```

**重要**: Minecraft 26.1.2+ 版本中，物品必须同时具备：
1. 模型文件 (`models/item/<item_id>.json`)
2. 客户端物品文件 (`items/<item_id>.json`)

缺少客户端物品文件会导致物品在物品栏、箱子中显示为紫黑方块。

## 语言文件

### 格式
```json
{
  "item.<modid>.<item_id>": "显示名称",
  "block.<modid>.<block_id>": "显示名称",
  "itemGroup.<modid>.<tab_id>": "创造模式标签名称",
  "container.<modid>.<container_id>": "容器界面标题"
}
```

### 示例 (zh_cn.json)
```json
{
  "item.relay.computing_core": "运算核心",
  "item.relay.spell_disk": "法术磁盘",
  "item.relay.energy_module": "能量模块",
  "block.relay.shell_block": "外壳",
  "itemGroup.relay.creative_tab": "Relay",
  "container.relay.shell": "外壳"
}
```

## 物品模型

### 标准物品模型 (2D)
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "<modid>:item/<item_id>"
  }
}
```

### 示例 (computing_core.json - models/item/)
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "relay:item/computing_core"
  }
}
```

## 客户端物品文件 (Client Item) - 26.1.2+ 必需

Minecraft 26.1.2+ 版本要求每个物品必须有一个客户端物品定义文件，位于 `assets/<modid>/items/` 目录下。

### 格式
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "<modid>:item/<item_id>"
  }
}
```

### 示例 (computing_core.json - items/)
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "relay:item/computing_core"
  }
}
```

### 字段说明
- `model.type`: 固定为 `minecraft:model`
- `model.model`: 指向物品模型的路径（不带 `.json` 扩展名）

### 完整物品资源包结构
```
assets/relay/
├── items/computing_core.json          # 客户端物品定义 (26.1.2+ 必需)
├── models/item/computing_core.json    # 物品模型
├── textures/item/computing_core.png   # 物品纹理
└── lang/zh_cn.json                    # 语言文件
```

## 方块模型

### 六面相同纹理的方块
```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "<modid>:block/<block_id>"
  }
}
```

### 示例 (shell_block.json - block 模型)
```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "relay:block/shell_block"
  }
}
```

### 物品栏中的方块模型 (item 模型)
```json
{
  "parent": "<modid>:block/<block_id>"
}
```

### 示例 (shell_block.json - item 模型)
```json
{
  "parent": "relay:block/shell_block"
}
```

## 方块状态文件 (Blockstate)

### 简单方块 (无变体)
```json
{
  "variants": {
    "": {
      "model": "<modid>:block/<block_id>"
    }
  }
}
```

### 示例 (shell_block.json - blockstate)
```json
{
  "variants": {
    "": {
      "model": "relay:block/shell_block"
    }
  }
}
```

## 纹理文件要求

| 属性 | 要求 |
|------|------|
| 格式 | PNG (无损压缩) |
| 尺寸 | 2 的幂次方：16x16, 32x32, 64x64, 128x128, 256x256 |
| 色彩模式 | RGBA (带透明度通道) |
| 命名 | 小写字母 + 下划线，如 `computing_core.png` |

## 常见问题排查

### 问题 1: 紫黑方块 (缺失纹理)

**症状**: 物品或方块显示为紫黑相间的棋盘格

**排查步骤**:

1. **检查纹理文件位置**
   ```bash
   # 物品纹理应在
   src/main/resources/assets/<modid>/textures/item/<item_id>.png

   # 方块纹理应在
   src/main/resources/assets/<modid>/textures/block/<block_id>.png
   ```

2. **检查模型 JSON 文件**
   - 确认 `parent` 路径正确
   - 确认 `textures` 中的路径不带 `.png` 扩展名
   - 确认命名空间正确 (通常是 `modid:` 前缀)

3. **检查客户端物品文件 (26.1.2+ 必需)**
   - 确认 `assets/<modid>/items/<item_id>.json` 存在
   - 确认 JSON 格式正确：
     ```json
     {
       "model": {
         "type": "minecraft:model",
         "model": "<modid>:item/<item_id>"
       }
     }
     ```
   - **注意**: 缺少此文件会导致物品在物品栏/箱子中显示紫黑块

4. **检查语言文件**
   - 确认翻译键格式：`item.<modid>.<id>` 或 `block.<modid>.<id>`
   - 确认 JSON 语法正确

5. **检查方块渲染形状 (BaseEntityBlock)**

   如果方块继承自 `BaseEntityBlock`，必须覆盖 `getRenderShape()` 方法：

   ```java
   @Override
   protected RenderShape getRenderShape(BlockState state) {
       return RenderShape.MODEL;  // 返回 MODEL 才能正确渲染纹理
   }
   ```

   **原因**: `BaseEntityBlock` 默认渲染形状是 `RenderShape.INVISIBLE`，不设置会导致方块不可见或显示紫黑块。

6. **验证文件完整性**
   ```bash
   # 检查 PNG 文件是否有效
   file src/main/resources/assets/<modid>/textures/item/*.png

   # 应输出：PNG image data, xxx x yyy, 8-bit/color RGBA, non-interlaced
   ```

7. **检查构建输出**
   ```bash
   ./gradlew build
   # 确认资源文件被正确打包到 JAR 中
   unzip -l build/libs/*.jar | grep "assets/<modid>"
   ```

### 问题 2: 物品在手中/地上显示正常但在创造模式/箱子中显示紫黑块

**原因**: 缺少物品模型文件

**解决**: 在 `models/item/` 目录下创建对应的 JSON 模型文件

### 问题 3: 方块放置后显示紫黑块但物品形式正常

**原因**: 
- 缺少方块模型文件 (`models/block/`)
- 缺少方块状态文件 (`blockstates/`)
- `BaseEntityBlock` 未设置正确的 `RenderShape`

**解决**: 检查上述三个文件是否都存在且配置正确

### 问题 4: 语言文件不生效 (显示翻译键而非文本)

**排查**:
1. 确认语言文件在 `assets/<modid>/lang/` 目录下
2. 确认文件名正确：`zh_cn.json` 或 `en_us.json`
3. 确认 JSON 语法正确 (使用 JSON 验证工具)
4. 确认翻译键与注册 ID 完全匹配

## 完整示例

### 物品完整资源包

```
assets/relay/
├── lang/zh_cn.json
│   {"item.relay.computing_core": "运算核心"}
├── textures/item/computing_core.png
└── models/item/computing_core.json
    {"parent": "minecraft:item/generated", "textures": {"layer0": "relay:item/computing_core"}}
```

### 方块完整资源包

```
assets/relay/
├── lang/zh_cn.json
│   {"block.relay.shell_block": "外壳"}
├── textures/block/shell_block.png
├── models/block/shell_block.json
│   {"parent": "minecraft:block/cube_all", "textures": {"all": "relay:block/shell_block"}}
├── models/item/shell_block.json
│   {"parent": "relay:block/shell_block"}
└── blockstates/shell_block.json
    {"variants": {"": {"model": "relay:block/shell_block"}}}
```

## 验证清单

在运行游戏前，确认以下项目：

- [ ] 纹理文件在正确的目录 (`textures/item/` 或 `textures/block/`)
- [ ] 纹理文件是有效的 PNG 格式
- [ ] 模型文件在正确的目录 (`models/item/` 或 `models/block/`)
- [ ] 模型 JSON 中的纹理路径不带 `.png` 扩展名
- [ ] **客户端物品文件存在** (`items/<item_id>.json`) - 26.1.2+ 必需
- [ ] 客户端物品文件的 `model.model` 指向正确的模型路径
- [ ] 方块有 `blockstates/<block_id>.json` 文件
- [ ] 语言文件包含所有物品和方块的翻译
- [ ] `BaseEntityBlock` 子类覆盖了 `getRenderShape()` 返回 `RenderShape.MODEL`
- [ ] 构建成功且资源文件被打包到 JAR 中
