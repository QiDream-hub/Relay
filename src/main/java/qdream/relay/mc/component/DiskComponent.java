package qdream.relay.mc.component;

import net.minecraft.world.item.ItemStack;

/**
 * 法术磁盘组件接口
 * 提供程序存储和读写方法（JSON 字符串存储）
 */
public interface DiskComponent {
    /**
     * 从磁盘读取程序（JSON 字符串）
     *
     * @param stack 物品堆
     * @return 程序 JSON 字符串，如果没有程序则返回 null
     */
    String getProgram(ItemStack stack);

    /**
     * 保存程序到磁盘（JSON 字符串）
     *
     * @param stack   物品堆
     * @param programJson 程序 JSON 字符串
     */
    void setProgram(ItemStack stack, String programJson);

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
