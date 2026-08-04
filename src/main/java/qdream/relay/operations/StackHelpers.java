package qdream.relay.operations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.StackException;
import qdream.relay.mc.errors.TypeException;
import qdream.relay.tools.StackTools;
import qdream.relay.types.NumberData;
import qdream.relay.types.BooleanData;
import qdream.relay.types.VectorData;
import qdream.relay.types.EntityData;
import qdream.relay.types.SlotData;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.StringData;
import qdream.relay.types.TypeData;

/**
 * 栈操作工具类 - 提供类型安全的栈弹出/窥视/检查方法
 *
 * <p>
 * 提供以下功能：
 * </p>
 * <ul>
 * <li>通用栈弹出与类型检查</li>
 * <li>类型安全的弹出/窥视方法（Number, Boolean, Vector, Entity, BlockEntity, List, String, Type, Slot）</li>
 * <li>栈索引访问（获取/设置/移除）</li>
 * <li>栈大小检查</li>
 * <li>便捷转换方法</li>
 * </ul>
 */
public final class StackHelpers {

    private StackHelpers() {
        // 防止实例化
    }

    // ==================== 通用栈弹出 ====================

    /**
     * 从数据栈弹出任意元素
     *
     * @param executor 状态机
     * @return Executable 非空
     * @throws StackException 如果栈空
     */
    @NotNull
    public static Executable popAny(StateMachine executor) {
        Executable popData = executor.popData();
        if (popData == null) {
            throw new StackException("数据栈不足");
        }
        return popData;
    }

    /**
     * 从数据栈弹出并检查类型
     *
     * @param executor      状态机
     * @param expectedType  期望的类型
     * @param operationName 操作名称（用于错误消息）
     * @param targetId      目标类型 ID（用于错误消息）
     * @param <T>           期望的类型
     * @return 转换后的值，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static <T> T popAsType(StateMachine executor, Class<T> expectedType, String operationName, String targetId) {
        Executable exe = executor.popData();
        if (exe == null) {
            throw new StackException(operationName + " 数据栈不足");
        }
        if (!expectedType.isInstance(exe)) {
            throw new TypeException(operationName + " 期望:" + targetId +
                    " 类型，实际为：" + StackTools.getId(exe));
        }
        return expectedType.cast(exe);
    }

    /**
     * 从数据栈窥视（不弹出）并检查类型
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param expectedType  期望的类型
     * @param operationName 操作名称（用于错误消息）
     * @param targetId      目标类型 ID（用于错误消息）
     * @param <T>           期望的类型
     * @return 转换后的值，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static <T> T peekAsType(StateMachine executor, int index, Class<T> expectedType, String operationName,
            String targetId) {
        Executable exe = getDataAt(executor, index, operationName);
        if (!expectedType.isInstance(exe)) {
            throw new TypeException(operationName + " 期望:" + targetId +
                    " 类型，实际为：" + StackTools.getId(exe));
        }
        return expectedType.cast(exe);
    }

    // ==================== 类型安全的弹出方法 ====================

    /**
     * 弹出 NumberData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return NumberData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static NumberData popNumber(StateMachine executor, String operationName) {
        return popAsType(executor, NumberData.class, operationName, "relay:number");
    }

    /**
     * 窥视栈顶 NumberData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return NumberData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static NumberData peekNumber(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, NumberData.class, operationName, "relay:number");
    }

    /**
     * 弹出 BooleanData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BooleanData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static BooleanData popBoolean(StateMachine executor, String operationName) {
        return popAsType(executor, BooleanData.class, operationName, "relay:boolean");
    }

    /**
     * 窥视栈顶 BooleanData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BooleanData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static BooleanData peekBoolean(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, BooleanData.class, operationName, "relay:boolean");
    }

    /**
     * 弹出 VectorData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return VectorData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static VectorData popVector(StateMachine executor, String operationName) {
        return popAsType(executor, VectorData.class, operationName, "relay:vector");
    }

    /**
     * 窥视栈顶 VectorData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return VectorData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static VectorData peekVector(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, VectorData.class, operationName, "relay:vector");
    }

    /**
     * 弹出 EntityData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return EntityData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static EntityData popEntity(StateMachine executor, String operationName) {
        return popAsType(executor, EntityData.class, operationName, "relay:entity");
    }

    /**
     * 窥视栈顶 EntityData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return EntityData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static EntityData peekEntity(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, EntityData.class, operationName, "relay:entity");
    }

    /**
     * 弹出 BlockEntityData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BlockEntityData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static BlockEntityData popBlockEntity(StateMachine executor, String operationName) {
        return popAsType(executor, BlockEntityData.class, operationName, "relay:block_entity");
    }

    /**
     * 窥视栈顶 BlockEntityData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BlockEntityData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static BlockEntityData peekBlockEntity(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, BlockEntityData.class, operationName, "relay:block_entity");
    }

    /**
     * 弹出 ListData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return ListData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static ListData popList(StateMachine executor, String operationName) {
        return popAsType(executor, ListData.class, operationName, "relay:list");
    }

    /**
     * 窥视栈顶 ListData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return ListData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static ListData peekList(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, ListData.class, operationName, "relay:list");
    }

    /**
     * 弹出 StringData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return StringData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static StringData popString(StateMachine executor, String operationName) {
        return popAsType(executor, StringData.class, operationName, "relay:string");
    }

    /**
     * 窥视栈顶 StringData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return StringData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static StringData peekString(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, StringData.class, operationName, "relay:string");
    }

    /**
     * 弹出 TypeData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return TypeData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static TypeData popType(StateMachine executor, String operationName) {
        return popAsType(executor, TypeData.class, operationName, "relay:type");
    }

    /**
     * 窥视栈顶 TypeData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return TypeData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static TypeData peekType(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, TypeData.class, operationName, "relay:type");
    }

    /**
     * 弹出 SlotData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return SlotData，非空
     * @throws StackException 如果栈空
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static SlotData popSlot(StateMachine executor, String operationName) {
        return popAsType(executor, SlotData.class, operationName, "relay:slot");
    }

    /**
     * 窥视栈顶 SlotData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return SlotData，非空
     * @throws StackException 如果索引越界
     * @throws TypeException  如果类型不匹配
     */
    @NotNull
    public static SlotData peekSlot(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, SlotData.class, operationName, "relay:slot");
    }

    // ==================== 便捷转换方法 ====================

    /**
     * 将 Executable 转换为 NumberData 并获取 double 值
     *
     * @param exe Executable
     * @return double 值，如果转换失败返回 0.0
     */
    public static double asDouble(Executable exe) {
        if (exe instanceof NumberData num) {
            return num.asDouble();
        }
        return 0.0;
    }

    /**
     * 将 Executable 转换为 BooleanData 并获取 boolean 值
     *
     * @param exe Executable
     * @return boolean 值，如果转换失败返回 false
     */
    public static boolean asBoolean(Executable exe) {
        if (exe instanceof BooleanData bool) {
            return bool.asBoolean();
        }
        return false;
    }

    /**
     * 将 Executable 转换为 VectorData 并获取 Vec3 值
     *
     * @param exe Executable
     * @return Vec3 值，如果转换失败返回 Vec3.ZERO
     */
    @NotNull
    public static net.minecraft.world.phys.Vec3 asVector(Executable exe) {
        if (exe instanceof VectorData vec) {
            return vec.asVector();
        }
        return net.minecraft.world.phys.Vec3.ZERO;
    }

    // ==================== 多参数弹出 ====================

    /**
     * 从数据栈弹出指定数量的参数
     *
     * @param executor 状态机
     * @param count    参数数量
     * @return Executable 数组，非空
     * @throws StackException 如果栈不足
     */
    @NotNull
    public static Executable[] popMultiple(StateMachine executor, int count) {
        Executable[] result = new Executable[count];
        for (int i = count - 1; i >= 0; i--) {
            result[i] = executor.popData();
            if (result[i] == null) {
                throw new StackException("数据栈不足，需要 " + count + " 个参数");
            }
        }
        return result;
    }

    /**
     * 检查数据栈是否有足够元素
     *
     * @param executor      状态机
     * @param required      需要的元素数量
     * @param operationName 操作名称
     * @throws StackException 如果栈不足
     */
    public static void checkStackSize(StateMachine executor, int required, String operationName) {
        if (executor.getDataStackSize() < required) {
            throw new StackException(operationName + " 需要 " + required + " 个参数");
        }
    }

    // ==================== 栈索引访问相关 ====================

    /**
     * 从数据栈获取指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 元素，非空
     * @throws StackException 如果索引无效
     */
    @NotNull
    public static Executable getDataAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= executor.getDataStackSize()) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
        Executable target = executor.getDataAt(index);
        if (target == null) {
            throw new StackException(operationName + " 无法获取目标元素");
        }
        return target;
    }

    /**
     * 从数据栈移除指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 被移除的元素，非空
     * @throws StackException 如果索引无效
     */
    @NotNull
    public static Executable removeDataAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= executor.getDataStackSize()) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
        Executable target = executor.removeDataAt(index);
        if (target == null) {
            throw new StackException(operationName + " 无法移除目标元素");
        }
        return target;
    }

    /**
     * 设置数据栈指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param value         新值
     * @param operationName 操作名称
     * @throws StackException 如果索引无效
     */
    public static void setDataAt(StateMachine executor, int index, Executable value, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= executor.getDataStackSize()) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
        if (!executor.setDataAt(index, value)) {
            throw new StackException(operationName + " 无法设置目标元素");
        }
    }

    /**
     * 从程序栈获取指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 元素，非空
     * @throws StackException 如果索引无效
     */
    @NotNull
    public static Executable getProgramAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= executor.getProgramStackSize()) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
        Executable target = executor.getProgramAt(index);
        if (target == null) {
            throw new StackException(operationName + " 无法获取目标元素");
        }
        return target;
    }

    /**
     * 从程序栈移除指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 被移除的元素，非空
     * @throws StackException 如果索引无效
     */
    @NotNull
    public static Executable removeProgramAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= executor.getProgramStackSize()) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
        Executable target = executor.removeProgramAt(index);
        if (target == null) {
            throw new StackException(operationName + " 无法移除目标元素");
        }
        return target;
    }

    /**
     * 设置程序栈指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param value         新值
     * @param operationName 操作名称
     * @throws StackException 如果索引无效
     */
    public static void setProgramAt(StateMachine executor, int index, Executable value, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= executor.getProgramStackSize()) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
        if (!executor.setProgramAt(index, value)) {
            throw new StackException(operationName + " 无法设置目标元素");
        }
    }

    /**
     * 验证索引是否有效
     *
     * @param executor      状态机
     * @param index         索引值
     * @param stackSize     栈大小
     * @param operationName 操作名称
     * @throws StackException 如果索引无效
     */
    public static void checkIndex(StateMachine executor, int index, int stackSize, String operationName) {
        if (index < 0) {
            throw new StackException(operationName + " 索引不能为负数");
        }
        if (index >= stackSize) {
            throw new StackException(operationName + " 索引超出栈范围");
        }
    }
}
