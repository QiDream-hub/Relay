package qdream.relay.items;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.StateMachineNbtSerializer;

/**
 * 工具外壳（手持物品形态）
 * 
 * <p>简化设计：ToolShellItem 仅负责右键交互逻辑，所有状态管理委托给 ToolShellContainer</p>
 * 
 * <h3>右键行为</h3>
 * <ul>
 *   <li>Shift+ 右键：停止程序（清空双栈）</li>
 *   <li>普通右键：
 *     <ul>
 *       <li>运行中：显示状态（程序栈、数据栈）</li>
 *       <li>已停止：从磁盘加载程序并运行</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class ToolShellItem extends Item {

    public ToolShellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 获取容器（状态管理的权威来源）
        ToolShellContainer container = new ToolShellContainer(this, stack);
        StateMachine machine = container.getStateMachine();

        // Shift+ 右键：停止程序
        if (player.isShiftKeyDown()) {
            machine.clear();
            container.saveStateMachine();
            player.sendSystemMessage(Component.literal("§c[工具外壳] 程序已停止"));
            return InteractionResult.SUCCESS;
        }

        // 普通右键：检查运行状态
        if (machine.isRunning()) {
            // 正在运行中，显示状态
            player.sendSystemMessage(Component.literal("§e[工具外壳] 程序正在运行中"));
            player.sendSystemMessage(
                    Component.literal("§e[程序栈] " + qdream.relay.commands.CommandUtils.dataStackToString(machine.getProgramStackSnapshot())));
            player.sendSystemMessage(
                    Component.literal("§e[数据序栈] " + qdream.relay.commands.CommandUtils.dataStackToString(machine.getDataStackSnapshot())));
        } else {
            // 已停止，加载磁盘程序并运行
            ItemStack diskStack = container.getDiskStack();
            if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
                List<Executable> program = SpellDiskItem.getProgram(diskStack);
                if (!program.isEmpty()) {
                    // 清空双栈后加载新程序
                    machine.clear();
                    machine.loadProgram(program);
                    container.saveStateMachine();
                    container.setInitialized(true);
                    player.sendSystemMessage(Component.literal("§a[工具外壳] 程序已启动，共 " + program.size() + " 个指令"));
                } else {
                    player.sendSystemMessage(Component.literal("§e[工具外壳] 磁盘为空，无法启动"));
                }
            } else {
                player.sendSystemMessage(Component.literal("§e[工具外壳] 未插入法术磁盘"));
            }
        }

        // 设置 Owner
        container.setOwner(player);

        return InteractionResult.SUCCESS;
    }

    /**
     * 获取工具外壳容器（状态管理的权威来源）
     */
    public ToolShellContainer getContainer(ItemStack stack) {
        return new ToolShellContainer(this, stack);
    }

    /**
     * 检查是否正在运行
     */
    public boolean isRunning(ItemStack stack) {
        ToolShellContainer container = getContainer(stack);
        return container.getStateMachine().isRunning();
    }
}
