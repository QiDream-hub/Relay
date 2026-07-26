package qdream.relay.tools;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 栈工具类 - 提供程序栈和数据栈的操作 ID 提取与格式化功能
 */
public final class StackTools {

    private StackTools() {
        // 防止实例化
    }

    // ==================== 可执行单元 ID 获取 ====================

    /**
     * 获取可执行单元的 ID
     * 优先尝试转换为 Data 或 Instruction 获取 ID，失败则返回简单类名
     *
     * @param exe 可执行单元
     * @return ID 或类名
     */
    public static String getId(Executable exe) {
        if (exe instanceof Data data) {
            return data.getId();
        } else if (exe instanceof Instruction instr) {
            return instr.getId();
        } else {
            return exe.getClass().getSimpleName();
        }
    }

    /**
     * 获取可执行单元的显示名称翻译键
     * 根据类型返回不同的翻译键：
     * - Data: "type.{id}.name"
     * - Instruction: "operation.{id}.name"
     *
     * @param exe 可执行单元
     * @return 翻译键
     */
    public static String getNameKey(Executable exe) {
        if (exe instanceof Data data) {
            return "type." + data.getId() + ".name";
        } else if (exe instanceof Instruction instr) {
            return "operation." + instr.getId() + ".name";
        } else {
            return exe.getClass().getSimpleName();
        }
    }

    // ==================== 通过 ID 获取显示名称 ====================

    /**
     * 获取类型 ID 的显示名称（从语言文件）
     * 用于编辑器等客户端 UI
     *
     * @param typeId 类型 ID（如 "relay:number"）
     * @return 显示名称，如果语言文件不存在则返回 ID
     */
    public static String getTypeDisplayName(String typeId) {
        String key = "type." + typeId + ".name";
        String name = net.minecraft.network.chat.Component.translatable(key).getString();
        return name.equals(key) ? typeId : name;
    }

    /**
     * 获取操作 ID 的显示名称（从语言文件）
     * 用于编辑器等客户端 UI
     *
     * @param opId 操作 ID（如 "relay:add"）
     * @return 显示名称，如果语言文件不存在则返回 ID
     */
    public static String getOperationDisplayName(String opId) {
        String key = "operation." + opId + ".name";
        String name = net.minecraft.network.chat.Component.translatable(key).getString();
        return name.equals(key) ? opId : name;
    }

    // ==================== 指令 ID 提取 ====================

    /**
     * 获取数据栈中所有指令的 ID 列表
     *
     * @param executor 状态机
     * @return 指令 ID 列表（只包含 Instruction 类型）
     */
    public static List<String> getDataStackInstructionIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getDataStackSnapshot()) {
            if (exe instanceof Instruction instr) {
                ids.add(instr.getId());
            }
        }
        return ids;
    }

    /**
     * 获取程序栈中所有指令的 ID 列表
     *
     * @param executor 状态机
     * @return 指令 ID 列表（只包含 Instruction 类型）
     */
    public static List<String> getProgramStackInstructionIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getProgramStackSnapshot()) {
            if (exe instanceof Instruction instr) {
                ids.add(instr.getId());
            }
        }
        return ids;
    }

    // ==================== 可执行单元 ID/类型名提取 ====================

    /**
     * 获取数据栈中所有可执行单元的 ID 或类型名
     *
     * @param executor 状态机
     * @return ID/类型名列表（Instruction 返回 ID，其他返回类名）
     */
    public static List<String> getDataStackIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getDataStackSnapshot()) {
            if (exe instanceof Instruction instr) {
                ids.add("§e" + instr.getId());
            } else {
                ids.add("§f" + exe.getClass().getSimpleName());
            }
        }
        return ids;
    }

    /**
     * 获取程序栈中所有可执行单元的 ID 或类型名
     *
     * @param executor 状态机
     * @return ID/类型名列表（Instruction 返回 ID，其他返回类名）
     */
    public static List<String> getProgramStackIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getProgramStackSnapshot()) {
            if (exe instanceof Instruction instr) {
                ids.add("§e" + instr.getId());
            } else {
                ids.add("§f" + exe.getClass().getSimpleName());
            }
        }
        return ids;
    }

    // ==================== 栈格式化 ====================

    /**
     * 格式化数据栈快照为字符串
     *
     * @param executor 状态机
     * @return 格式化后的字符串
     */
    public static String formatDataStack(StateMachine executor) {
        return formatStack(executor.getDataStackSnapshot());
    }

    /**
     * 格式化程序栈快照为字符串
     *
     * @param executor 状态机
     * @return 格式化后的字符串
     */
    public static String formatProgramStack(StateMachine executor) {
        return formatStack(executor.getProgramStackSnapshot());
    }

    /**
     * 格式化栈快照为字符串
     *
     * @param stack 栈快照
     * @return 格式化后的字符串
     */
    public static String formatStack(List<Executable> stack) {
        if (stack.isEmpty()) {
            return "§8[]";
        }
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                sb.append("§8, ");
            }
            Executable exe = stack.get(i);
            if (exe instanceof Instruction instr) {
                sb.append("§e").append(instr.getId());
            } else {
                sb.append("§f").append(exe.getClass().getSimpleName());
            }
        }
        sb.append("§8]");
        return sb.toString();
    }
}
