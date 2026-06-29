package qdream.relay.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.items.ToolShellContainer;
import qdream.relay.items.ToolShellItem;

/**
 * 玩家工具外壳数据
 *
 * <p>使用 {@link Map}&lt;UUID, ToolShellContainer&gt; 缓存活跃的容器，以会话 ID 为 Key</p>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>右键时生成会话 ID（UUID）并存储到 ItemStack 的 DataComponent</li>
 *   <li>以会话 ID 为 Key 查找 Container，完全不依赖 ItemStack 引用或内容</li>
 *   <li>玩家 tick 时批量执行所有 Container</li>
 *   <li>程序执行完毕后自动移除并保存</li>
 * </ul>
 *
 * <h3>生命周期</h3>
 * <pre>
 * 玩家右键
 *   └─→ 从 ItemStack 读取会话 ID
 *        ├─ 有 → 从 Map 获取 Container
 *        └─ 无 → 生成新 UUID，存储到 ItemStack，创建 Container 加入 Map
 *
 * 玩家 tick
 *   └─→ 遍历 Map 中所有 Container
 *        ├─ 程序运行中 → tick()
 *        └─ 程序已结束 → 从 Map 移除并保存
 *
 * 玩家下线/物品丢弃
 *   └─→ 所有 Container 保存并清空
 * </pre>
 */
public class PlayerShellData {

    /**
     * 活跃的 ToolShellContainer 缓存
     * Key: 会话 ID (UUID)
     * Value: ToolShellContainer 实例
     */
    private final Map<UUID, ToolShellContainer> activeShells = new HashMap<>();

    /**
     * 所属玩家
     */
    private final Player player;

    public PlayerShellData(Player player) {
        this.player = player;
    }

    /**
     * 从 ItemStack 获取会话 ID
     *
     * @param stack ItemStack
     * @return 会话 ID，不存在返回 null
     */
    private UUID getSessionId(ItemStack stack) {
        String sessionIdStr = stack.get(RelayDataComponents.TOOL_SHELL_SESSION_ID);
        if (sessionIdStr == null) {
            return null;
        }
        try {
            return UUID.fromString(sessionIdStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 为 ItemStack 生成并存储新会话 ID
     *
     * @param stack ItemStack
     * @return 新生成的会话 ID
     */
    private UUID createSessionId(ItemStack stack) {
        UUID sessionId = UUID.randomUUID();
        stack.set(RelayDataComponents.TOOL_SHELL_SESSION_ID, sessionId.toString());
        return sessionId;
    }

    /**
     * 恢复 Container（玩家上线时调用）
     * <p>从 ItemStack 加载状态并恢复到 Map 中</p>
     *
     * @param stack ItemStack
     * @param sessionId 会话 ID
     * @return ToolShellContainer 实例
     */
    public ToolShellContainer restoreContainer(ItemStack stack, UUID sessionId) {
        if (stack.getItem() instanceof ToolShellItem toolShell) {
            ToolShellContainer container = new ToolShellContainer(toolShell, stack, sessionId);
            activeShells.put(sessionId, container);
            return container;
        }
        return null;
    }

    /**
     * 获取或创建 ToolShellContainer
     *
     * @param stack 工具外壳 ItemStack
     * @return ToolShellContainer 实例
     */
    public ToolShellContainer getOrCreateContainer(ItemStack stack) {
        UUID sessionId = getSessionId(stack);
        
        // 已有会话 ID，直接从 Map 获取
        if (sessionId != null) {
            ToolShellContainer existing = activeShells.get(sessionId);
            if (existing != null) {
                return existing;
            }
        }
        
        // 没有会话 ID 或 Map 中不存在，创建新的
        if (stack.getItem() instanceof ToolShellItem toolShell) {
            // 生成新会话 ID
            UUID newSessionId = createSessionId(stack);
            ToolShellContainer container = new ToolShellContainer(toolShell, stack, newSessionId);
            activeShells.put(newSessionId, container);
            return container;
        }
        return null;
    }

    /**
     * 检查是否存在 Container
     *
     * @param stack ItemStack
     * @return 是否存在
     */
    public boolean hasContainer(ItemStack stack) {
        UUID sessionId = getSessionId(stack);
        return sessionId != null && activeShells.containsKey(sessionId);
    }

    /**
     * 停止并移除 Container
     * <p>用于 Shift+ 右键停止程序</p>
     *
     * @param stack ItemStack
     */
    public void stopContainer(ItemStack stack) {
        UUID sessionId = getSessionId(stack);
        if (sessionId != null) {
            ToolShellContainer container = activeShells.remove(sessionId);
            if (container != null) {
                container.getStateMachine().clear();
                container.saveAllState();
                // 清除 ItemStack 中的会话 ID
                stack.remove(RelayDataComponents.TOOL_SHELL_SESSION_ID);
            }
        }
    }

    /**
     * 移除 Container
     *
     * @param sessionId 会话 ID
     */
    public void removeContainer(UUID sessionId) {
        ToolShellContainer container = activeShells.remove(sessionId);
        if (container != null) {
            container.saveAllState();
        }
    }

    /**
     * Tick 所有活跃的 Container
     * <p>程序执行完毕的 Container 会自动从 Map 移除并保存</p>
     */
    public void tickAll() {
        if (player.level().isClientSide()) {
            return;
        }

        // 收集需要移除的会话 ID（程序已执行的）
        // 不能直接在循环中 remove，会 ConcurrentModificationException
        var toRemove = new java.util.ArrayList<UUID>();

        for (var entry : activeShells.entrySet()) {
            UUID sessionId = entry.getKey();
            ToolShellContainer container = entry.getValue();
            ItemStack stack = container.getStack();

            // 检查物品是否仍然有效（未被丢弃）
            if (stack.isEmpty() || !(stack.getItem() instanceof ToolShellItem)) {
                toRemove.add(sessionId);
                continue;
            }

            // 执行 tick
            container.tick(player.level(), player);

            // 程序执行完毕后标记移除
            if (!container.getStateMachine().isRunning()) {
                toRemove.add(sessionId);
            }
        }

        // 移除已完成的 Container 并保存
        for (UUID sessionId : toRemove) {
            ToolShellContainer container = activeShells.remove(sessionId);
            if (container != null) {
                container.saveAllState(); // 保存最终状态
                // 清除 ItemStack 中的会话 ID
                container.getStack().remove(RelayDataComponents.TOOL_SHELL_SESSION_ID);
            }
        }
    }

    /**
     * 清空所有 Container（玩家下线时调用）
     * <p>停止所有运行中的程序并保存状态</p>
     */
    public void clear() {
        // 停止所有运行中的程序
        for (ToolShellContainer container : activeShells.values()) {
            container.saveAllState();
            // 清除 ItemStack 中的会话 ID
            // container.getStack().remove(RelayDataComponents.TOOL_SHELL_SESSION_ID);
        }
        activeShells.clear();
    }

    /**
     * 获取活跃的 Container 数量（用于调试）
     */
    public int getActiveCount() {
        return activeShells.size();
    }
}
