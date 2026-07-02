package qdream.relay.mc.component;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.Executable;

/**
 * 法术磁盘组件接口
 * 提供程序存储和读写方法
 */
public interface DiskComponent {
    /**
     * 从磁盘读取程序
     *
     * @param stack 物品堆
     * @return 程序列表，如果没有程序则返回空列表
     */
    List<Executable> getProgram(ItemStack stack);

    /**
     * 保存程序到磁盘
     *
     * @param stack   物品堆
     * @param program 程序列表
     */
    void setProgram(ItemStack stack, List<Executable> program);

    /**
     * 从状态机保存状态
     * 保存程序栈和数据栈的完整状态
     *
     * @param stack   物品堆
     * @param program 程序列表
     */
    void saveFromStateMachine(ItemStack stack, List<Executable> program);

    /**
     * 加载状态到状态机
     * 恢复程序栈
     *
     * @param stack   物品堆
     * @param machine 状态机
     */
    void loadToStateMachine(ItemStack stack, qdream.relay.engine.StateMachine machine);

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

    /**
     * 导出磁盘程序为 JSON 字符串
     *
     * @param stack 物品堆
     * @return JSON 字符串，如果没有程序则返回 "[]"
     */
    String exportToJson(ItemStack stack);

    /**
     * 从 JSON 字符串导入程序到磁盘
     *
     * @param stack   物品堆
     * @param jsonStr JSON 字符串
     * @throws qdream.relay.mc.ProgramCompiler.CompilationException 解析错误
     */
    void importFromJson(ItemStack stack, String jsonStr) throws qdream.relay.mc.ProgramCompiler.CompilationException;
}
