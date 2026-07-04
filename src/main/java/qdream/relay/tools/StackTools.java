package qdream.relay.tools;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;

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
     * 优先尝试转换为 Operation 获取 ID，失败则返回简单类名
     *
     * @param exe 可执行单元
     * @return ID 或类名
     */
    public static String getId(Executable exe) {
        if (exe instanceof Operation op) {
            return op.getId();
        }
        return exe.getClass().getSimpleName();
    }

    // ==================== 操作 ID 提取 ====================

    /**
     * 获取数据栈中所有操作的 ID 列表
     *
     * @param executor 状态机
     * @return 操作 ID 列表（只包含 Operation 类型）
     */
    public static List<String> getDataStackOperationIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getDataStackSnapshot()) {
            if (exe instanceof Operation op) {
                ids.add(op.getId());
            }
        }
        return ids;
    }

    /**
     * 获取程序栈中所有操作的 ID 列表
     *
     * @param executor 状态机
     * @return 操作 ID 列表（只包含 Operation 类型）
     */
    public static List<String> getProgramStackOperationIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getProgramStackSnapshot()) {
            if (exe instanceof Operation op) {
                ids.add(op.getId());
            }
        }
        return ids;
    }

    // ==================== 可执行单元 ID/类型名提取 ====================

    /**
     * 获取数据栈中所有可执行单元的 ID 或类型名
     *
     * @param executor 状态机
     * @return ID/类型名列表（Operation 返回 ID，其他返回类名）
     */
    public static List<String> getDataStackIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getDataStackSnapshot()) {
            if (exe instanceof Operation op) {
                ids.add("§e" + op.getId());
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
     * @return ID/类型名列表（Operation 返回 ID，其他返回类名）
     */
    public static List<String> getProgramStackIds(StateMachine executor) {
        List<String> ids = new ArrayList<>();
        for (Executable exe : executor.getProgramStackSnapshot()) {
            if (exe instanceof Operation op) {
                ids.add("§e" + op.getId());
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
            if (exe instanceof Operation op) {
                sb.append("§e").append(op.getId());
            } else {
                sb.append("§f").append(exe.getClass().getSimpleName());
            }
        }
        sb.append("§8]");
        return sb.toString();
    }
}
