package qdream.relay.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.mc.base.Operation;
import qdream.relay.engine.StateMachine;

/**
 * 法术磁盘物品
 * 存储栈图程序（Iota 列表）
 * 使用 26.1.2 DataComponent 系统，底层使用 NBT 格式存储
 */
public class SpellDiskItem extends Item {

    public SpellDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 从磁盘读取程序
     * @return 程序列表，如果没有程序则返回空列表
     */
    public static List<Executable> getProgram(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return List.of();
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        if (listOpt.isEmpty()) {
            return List.of();
        }
        ListTag listTag = listOpt.get();
        List<Executable> result = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            Optional<CompoundTag> elementOpt = listTag.getCompound(i);
            elementOpt.ifPresent(tag -> {
                String id = tag.getString("id").orElse("");
                OperationRegistry.getEntry(id).ifPresent(entry -> {
                    Operation instance = (Operation) entry.create();
                    result.add(instance.fromNbt(tag));
                });
            });
        }
        return result;
    }

    /**
     * 保存程序到磁盘
     * @param stack 物品堆
     * @param program 程序列表
     */
    public static void setProgram(ItemStack stack, List<Executable> program) {
        CompoundTag programTag = new CompoundTag();
        ListTag listTag;
        try {
            listTag = ProgramCompiler.toNbt(program);
        } catch (CompilationException e) {
            listTag = new ListTag();
            e.printStackTrace();
        }
        programTag.put("program", listTag);
        stack.set(RelayDataComponents.SPELL_PROGRAM, programTag);
    }

    /**
     * 从状态机保存状态
     * 保存程序栈和数据栈的完整状态
     * @param stack 物品堆
     * @param machine 状态机
     */
    public static void saveFromStateMachine(ItemStack stack, StateMachine machine) {
        // 获取程序栈快照
        List<Executable> programStack = machine.getProgramStackSnapshot();
        // 反转回原始顺序（快照是栈顺序，需要转为列表顺序）
        List<Executable> program = new ArrayList<>(programStack);
        java.util.Collections.reverse(program);

        // 保存程序
        setProgram(stack, program);
    }

    /**
     * 加载状态到状态机
     * 恢复程序栈和数据栈
     * @param stack 物品堆
     * @param machine 状态机
     */
    public static void loadToStateMachine(ItemStack stack, StateMachine machine) {
        List<Executable> program = getProgram(stack);
        if (!program.isEmpty()) {
            machine.loadProgram(program);
        }
    }

    /**
     * 检查磁盘是否有程序
     * @param stack 物品堆
     * @return 是否有程序
     */
    public static boolean hasProgram(ItemStack stack) {
        return stack.has(RelayDataComponents.SPELL_PROGRAM);
    }

    /**
     * 获取程序大小
     * @param stack 物品堆
     * @return 程序元素数量
     */
    public static int getProgramSize(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return 0;
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        return listOpt.map(ListTag::size).orElse(0);
    }

    /**
     * 清空磁盘
     * @param stack 物品堆
     */
    public static void clear(ItemStack stack) {
        stack.remove(RelayDataComponents.SPELL_PROGRAM);
    }

    /**
     * 导出磁盘程序为 JSON 字符串
     * @param stack 物品堆
     * @return JSON 字符串，如果没有程序则返回 "[]"
     */
    public static String exportToJson(ItemStack stack) {
        List<Executable> program = getProgram(stack);
        return ProgramCompiler.toJsonString(program);
    }

    /**
     * 从 JSON 字符串导入程序到磁盘
     * @param stack 物品堆
     * @param jsonStr JSON 字符串
     * @throws ProgramCompiler.CompilationException 解析错误
     */
    public static void importFromJson(ItemStack stack, String jsonStr) throws ProgramCompiler.CompilationException {
        List<Executable> program = ProgramCompiler.compileFromJson(jsonStr);
        setProgram(stack, program);
    }
}
