package qdream.relay.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import qdream.relay.engine.IExecutable;
import qdream.relay.mc.McIota;
import qdream.relay.engine.StateMachine;

/**
 * 法术磁盘物品
 * 存储栈图程序（Iota 列表）
 * 使用 26.1.2 DataComponent 系统实现
 */
public class SpellDiskItem extends Item {

    public SpellDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 从磁盘读取程序
     */
    public static List<IExecutable> getProgram(ItemStack stack) {
        return RelayDataComponents.getProgram(stack);
    }

    /**
     * 保存程序到磁盘
     */
    public static void setProgram(ItemStack stack, List<IExecutable> program) {
        RelayDataComponents.setProgram(stack, program);
    }

    /**
     * 从状态机保存状态
     * 保存程序栈和数据栈的完整状态
     */
    public static void saveFromStateMachine(ItemStack stack, StateMachine machine) {
        // 获取程序栈快照
        List<IExecutable> programStack = machine.getProgramStackSnapshot();
        // 反转回原始顺序（快照是栈顺序，需要转为列表顺序）
        List<IExecutable> program = new ArrayList<>(programStack);
        java.util.Collections.reverse(program);
        
        // 保存程序
        setProgram(stack, program);
    }

    /**
     * 加载状态到状态机
     * 恢复程序栈和数据栈
     */
    public static void loadToStateMachine(ItemStack stack, StateMachine machine) {
        List<IExecutable> program = getProgram(stack);
        if (program != null && !program.isEmpty()) {
            machine.loadProgram(program);
        }
    }

    /**
     * 检查磁盘是否有程序
     */
    public static boolean hasProgram(ItemStack stack) {
        return RelayDataComponents.hasProgram(stack);
    }

    /**
     * 获取程序大小
     */
    public static int getProgramSize(ItemStack stack) {
        return RelayDataComponents.getProgramSize(stack);
    }

    /**
     * 清空磁盘
     */
    public static void clear(ItemStack stack) {
        RelayDataComponents.clear(stack);
    }
}
