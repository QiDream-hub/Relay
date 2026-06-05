---
name: GUI 架构选择 - AbstractContainerScreen
description: 法术编辑器使用 AbstractContainerScreen 而非 Screen 基类的原因
type: project
---

**决策：** 法术编辑器 (SpellEditorScreen) 使用 `AbstractContainerScreen` 而非 `Screen` 基类。

**Why:** 26.1.2 的 `MenuScreens.register()` API 要求注册的 Screen 必须实现 `MenuAccess<M>` 接口。`AbstractContainerScreen` 已经实现了此接口，而纯 `Screen` 基类没有。如果强行使用 `Screen` 基类，需要完全自定义 GUI 打开方式（如通过自定义网络包），这会增加复杂度。

**How to apply:** 
- 所有通过 `MenuType` 和 `MenuScreens.register()` 打开的 GUI 必须使用 `AbstractContainerScreen` 或直接实现 `MenuAccess`
- `AbstractContainerScreen` 提供现成的 Slot 系统、物品栏、背景渲染等基础设施
- 即使不使用 Slot 功能，也需要继承 `AbstractContainerScreen` 以兼容 26.1.2 的 MenuScreens 系统

**技术细节（26.1.2）：**
- `AbstractContainerScreen<T extends AbstractContainerMenu>` 是泛型类
- 构造函数：`AbstractContainerScreen(T menu, Inventory inventory, Component title)`
- 渲染方法：`extractRenderState(GuiGraphicsExtractor, int, int, float)`
- 文本渲染：`graphics.text(Font, String, int, int, int)` (无 boolean 参数)
