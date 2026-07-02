package qdream.relay.items;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;

import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.core.PlayerShellDataAccessor;
import qdream.relay.mc.component.SpellDiskComponent;

/**
 * 工具外壳（手持物品形态）
 *
 * <p>
 * 简化设计：ToolShellItem 仅负责右键交互逻辑，所有状态管理委托给 ToolShellContainer
 * </p>
 *
 * <h3>右键行为</h3>
 * <ul>
 * <li>Shift+ 右键：停止程序（清空双栈并从玩家缓存移除）</li>
 * <li>普通右键：
 * <ul>
 * <li>运行中：显示状态（程序栈、数据栈）</li>
 * <li>已停止：创建 ToolShellContainer 并加入玩家缓存，加载磁盘程序并运行</li>
 * </ul>
 * </li>
 * </ul>
 */
public class ToolShellItem extends Item {

    public ToolShellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
            Consumer<Component> textConsumer, TooltipFlag type) {
        // 基础提示（服务端侧）
        textConsumer.accept(
                Component.translatable("item.relay.tool_shell.usage_right").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(
                Component.translatable("item.relay.tool_shell.usage_shift").withStyle(ChatFormatting.GRAY));
        // GUI 按键提示 - 使用 Component.keybind 动态显示按键绑定
        textConsumer.accept(
                Component.translatable("item.relay.tool_shell.usage_gui",
                        Component.keybind("key.relay.open_tool_shell_config"))
                        .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 获取玩家的 PlayerShellData
        if (!(player instanceof PlayerShellDataAccessor accessor)) {
            return InteractionResult.FAIL;
        }
        var shellData = accessor.relay$getShellData();

        // Shift+ 右键：停止程序
        if (player.isShiftKeyDown()) {
            shellData.stopContainer(stack);
            player.sendSystemMessage(Component.literal("§c[工具外壳] 程序已停止"));
            return InteractionResult.SUCCESS;
        }

        // 普通右键：获取或创建 Container
        ToolShellContainer container = shellData.getOrCreateContainer(stack);
        StateMachine machine = container.getStateMachine();

        // 设置 Owner
        container.setOwner(player);

        // 检查运行状态
        if (machine.isRunning()) {
            // 正在运行中
            // 开启调试输出时，显示程序栈和数据栈
            if (container.isDebugOutputEnabled()) {
                player.sendSystemMessage(Component.literal("§e[工具外壳] 程序正在运行中"));
                player.sendSystemMessage(
                        Component.literal("§e[程序栈] "
                                + qdream.relay.commands.CommandUtils
                                        .dataStackToString(machine.getProgramStackSnapshot())));
                player.sendSystemMessage(
                        Component.literal("§e[数据序栈] "
                                + qdream.relay.commands.CommandUtils
                                        .dataStackToString(machine.getDataStackSnapshot())));
            }
        } else {
            // 已停止，加载磁盘程序并运行
            ItemStack diskStack = container.getDiskStack();
            SpellDiskComponent diskComponent = getDiskComponent(diskStack);
            if (diskComponent != null) {
                List<Executable> program = diskComponent.getProgram(diskStack);
                if (!program.isEmpty()) {
                    // 清空双栈后加载新程序
                    machine.clear();
                    machine.loadProgram(program);
                    container.setInitialized(true);
                    // 不立即保存，让 tick 管理
                    if (container.isDebugOutputEnabled()) {
                        player.sendSystemMessage(Component.literal("§a[工具外壳] 程序已启动，共 " + program.size() + " 个指令"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§e[工具外壳] 磁盘为空，无法启动"));
                }
            } else {
                player.sendSystemMessage(Component.literal("§e[工具外壳] 未插入法术磁盘"));
            }
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 获取工具外壳容器（状态管理的权威来源）
     * <p>
     * 从 PlayerShellData 获取缓存的 Container
     * </p>
     *
     * @param stack  ItemStack
     * @param player 玩家实体（用于获取 PlayerShellData）
     * @return ToolShellContainer 实例，不存在返回 null
     */
    public ToolShellContainer getContainer(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (player instanceof PlayerShellDataAccessor accessor) {
            return accessor.relay$getShellData().getContainer(stack);
        }
        return null;
    }

    /**
     * 检查是否正在运行
     *
     * @param stack  ItemStack
     * @param player 玩家实体（用于获取 PlayerShellData）
     * @return 是否正在运行
     */
    public boolean isRunning(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        ToolShellContainer container = getContainer(stack, player);
        if (container == null) {
            return false;
        }
        return container.getStateMachine().isRunning();
    }

    /**
     * 从物品堆获取 SpellDiskComponent
     * @param stack 物品堆
     * @return SpellDiskComponent 实例，如果物品不是法术磁盘则返回 null
     */
    private SpellDiskComponent getDiskComponent(ItemStack stack) {
        if (stack.getItem() instanceof SpellDiskComponent) {
            return (SpellDiskComponent) stack.getItem();
        }
        return null;
    }
}
