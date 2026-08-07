package qdream.relay.mc.component;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

/**
 * 法术磁盘组件接口
 * 提供程序存储和读写方法
 */
public interface DiskComponent {
    /**
     * 从磁盘读取程序
     *
     * @param stack 物品堆
     * @return 程序列表的 ListTag，如果没有程序则返回空 ListTag
     */
    ListTag getProgram(ItemStack stack);

    /**
     * 保存程序到磁盘
     *
     * @param stack   物品堆
     * @param program 程序列表的 ListTag
     */
    void setProgram(ItemStack stack, ListTag program);

    /**
     * 检查磁盘是否有程序
     *
     * @param stack 物品堆
     * @return 是否有程序
     */
    boolean hasProgram(ItemStack stack);

    /**
     * 获取程序大小
     *
     * @param stack 物品堆
     * @return 程序元素数量
     */
    int getProgramSize(ItemStack stack);

    /**
     * 清空磁盘
     *
     * @param stack 物品堆
     */
    void clear(ItemStack stack);
}
