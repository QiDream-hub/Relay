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
 * 注意：26.1.2 使用 DataComponent 系统，这里暂时使用简化实现
 */
public class SpellDiskItem extends Item {

    public SpellDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 从磁盘读取程序
     * TODO: 使用 DataComponent 系统实现
     */
    public static List<IExecutable> getProgram(ItemStack stack) {
        // 临时实现：返回空列表
        return List.of();
    }

    /**
     * 保存程序到磁盘
     * TODO: 使用 DataComponent 系统实现
     */
    public static void setProgram(ItemStack stack, List<IExecutable> program) {
        // 临时实现：不保存
    }

    /**
     * 从状态机保存状态
     * TODO: 使用 DataComponent 系统实现
     */
    public static void saveFromStateMachine(ItemStack stack, StateMachine machine) {
        // 临时实现：不保存
    }

    /**
     * 加载状态到状态机
     * TODO: 使用 DataComponent 系统实现
     */
    public static void loadToStateMachine(ItemStack stack, StateMachine machine) {
        // 临时实现：不加载
    }

    /**
     * 检查磁盘是否有程序
     */
    public static boolean hasProgram(ItemStack stack) {
        return false;
    }

    /**
     * 获取程序大小
     */
    public static int getProgramSize(ItemStack stack) {
        return 0;
    }

    /**
     * 清空磁盘
     */
    public static void clear(ItemStack stack) {
        // 临时实现
    }
}
