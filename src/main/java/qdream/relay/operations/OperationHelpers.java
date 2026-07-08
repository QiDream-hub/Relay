package qdream.relay.operations;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.component.WorldInteractorComponent;
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

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 操作工具类 - 提取操作中重复使用的公共逻辑
 * 
 * <p>
 * 提供以下功能：
 * </p>
 * <ul>
 * <li>世界交互器检查与范围验证</li>
 * <li>上下文获取（Level, ShellContainer, Entity, BlockEntity）</li>
 * <li>类型安全的栈弹出与类型检查</li>
 * <li>常见类型转换</li>
 * </ul>
 */
public final class OperationHelpers {

    private OperationHelpers() {
        // 防止实例化
    }

    // ==================== 世界交互器相关 ====================

    /**
     * 从状态机获取 ShellContainer
     *
     * @param executor 状态机
     * @return ShellContainer，如果不存在返回 null
     */
    public static ShellContainer getShellContainer(StateMachine executor) {
        if (!executor.hasContext("shellContainer")) {
            return null;
        }
        return executor.getContext("shellContainer", ShellContainer.class).orElse(null);
    }

    /**
     * 检查世界交互器是否存在
     *
     * @param executor      状态机
     * @param operationName 操作名称（用于错误消息）
     * @return 如果存在返回 true，否则触发事故并返回 false
     */
    public static boolean checkWorldInteractor(StateMachine executor, String operationName) {
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap(operationName + " 需要世界交互器");
            return false;
        }
        return true;
    }

    /**
     * 检查世界交互器并扣除能量
     *
     * @param executor      状态机
     * @param operationName 操作名称（用于错误消息）
     * @param baseCost      基础能量消耗
     * @param rangeCost     范围系数消耗（由交互器品阶决定）
     * @return 如果检查通过并成功扣除能量返回 true，否则触发事故并返回 false
     */
    public static boolean checkWorldInteractorWithCost(
            StateMachine executor,
            String operationName,
            double baseCost,
            double rangeCost) {
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap(operationName + " 需要世界交互器");
            return false;
        }

        // 计算总消耗
        double totalCost = baseCost + rangeCost;

        // 检查并扣除能量
        if (!container.consumeEnergy(totalCost)) {
            executor.triggerMishap(operationName + " 能量不足：需要 " + totalCost);
            return false;
        }

        return true;
    }

    /**
     * 检查能量并扣除（用于动态消耗操作）
     * <p>
     * 此方法会自动加上操作的基础能量消耗
     * </p>
     *
     * @param executor      状态机
     * @param operationName 操作名称（用于错误消息）
     * @param dynamicEnergy 动态能量值（不包含基础消耗）
     * @return 如果能量充足并成功扣除返回 true，否则触发事故并返回 false
     */
    public static boolean checkEnergy(StateMachine executor, String operationName, double dynamicEnergy) {
        ShellContainer container = getShellContainer(executor);
        if (container == null) {
            executor.triggerMishap(operationName + " 无法获取容器上下文");
            return false;
        }

        // 获取操作的基础能量消耗
        Executable top = executor.peekProgram();
        double baseEnergy = 0;
        if (top instanceof Spell spell) {
            baseEnergy = spell.getEnergy();
        }

        // 总消耗 = 基础 + 动态
        double totalEnergy = baseEnergy + dynamicEnergy;

        if (!container.consumeEnergy(totalEnergy)) {
            executor.triggerMishap(operationName + " 能量不足：需要 " + totalEnergy);
            return false;
        }

        return true;
    }

    /**
     * 获取世界交互器物品栈
     * 
     * @param executor 状态机
     * @return 世界交互器物品栈，如果不存在返回空 Optional
     */
    public static Optional<ItemStack> getWorldInteractorStack(StateMachine executor) {
        ShellContainer container = getShellContainer(executor);
        if (container == null) {
            return Optional.empty();
        }
        return Optional.of(container.getInteractorStack());
    }

    /**
     * 检查目标位置是否在世界交互器范围内
     * 
     * @param executor      状态机
     * @param operationName 操作名称
     * @param sourcePos     源位置
     * @param targetPos     目标位置
     * @return 如果在范围内返回 true，否则返回 false
     */
    public static boolean checkInRange(StateMachine executor, String operationName,
            Vec3 sourcePos,
            Vec3 targetPos) {
        Optional<ItemStack> interactorOpt = getWorldInteractorStack(executor);
        if (interactorOpt.isEmpty()) {
            executor.triggerMishap(operationName + " 需要世界交互器");
            return false;
        }
        ItemStack interactor = interactorOpt.get();
        if (!(interactor.getItem() instanceof WorldInteractorComponent component)) {
            executor.triggerMishap(operationName + " 物品不是有效的世界交互器");
            return false;
        }
        if (!component.isInRange(interactor, sourcePos, targetPos)) {
            executor.triggerMishap(operationName + " 超出世界交互器范围");
            return false;
        }
        return true;
    }

    // ==================== 上下文获取 ====================

    /**
     * 从状态机获取 Level 上下文
     * 
     * @param executor      状态机
     * @param operationName 操作名称（用于错误消息）
     * @return Level，如果不存在触发事故并返回 Optional.empty()
     */
    public static Optional<Level> getLevel(StateMachine executor, String operationName) {
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap(operationName + " 无法获取世界");
        }
        return levelOpt;
    }

    /**
     * 获取执行者自身引用（Entity 或 BlockEntity）
     * 
     * @param executor 状态机
     * @return self 引用，可能为 Entity、BlockEntity 或 null
     */
    public static Object getSelf(StateMachine executor) {
        return executor.getContext("self", Object.class).orElse(null);
    }

    /**
     * 获取执行者自身位置
     * 
     * @param executor 状态机
     * @return 自身位置，如果无法获取返回 (0,0,0)
     */
    public static Vec3 getSelfPosition(StateMachine executor) {
        Object self = getSelf(executor);
        if (self instanceof Entity entity) {
            return entity.position();
        } else if (self instanceof BlockEntity blockEntity) {
            net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
            return Vec3.atCenterOf(pos);
        }
        return new Vec3(0, 0, 0);
    }

    // ==================== 类型安全的栈弹出 ====================

    public static Executable popAny(StateMachine executor) {
        Executable popData = executor.popData();
        if (popData == null) {
            executor.triggerMishap(" 数据栈不足");
            return null;
        }
        return popData;
    }

    /**
     * 从数据栈弹出并检查类型
     *
     * @param executor      状态机
     * @param expectedType  期望的类型
     * @param operationName 操作名称（用于错误消息）
     * @param <T>           期望的类型
     * @return 转换后的值，如果失败触发事故并返回 null
     */
    public static <T> T popAsType(StateMachine executor, Class<T> expectedType, String operationName, String targetId) {
        Executable exe = executor.popData();
        if (exe == null) {
            executor.triggerMishap(operationName + " 数据栈不足");
            return null;
        }
        if (!expectedType.isInstance(exe)) {
            executor.triggerMishap(operationName + " 期望:" + targetId +
                    " 类型，实际为：" + StackTools.getId(exe));
            return null;
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
     * @param <T>           期望的类型
     * @return 转换后的值，如果失败触发事故并返回 null
     */
    public static <T> T peekAsType(StateMachine executor, int index, Class<T> expectedType, String operationName,
            String targetId) {
        Executable exe = getDataAt(executor, index, operationName);
        if (exe == null) {
            return null;
        }
        if (!expectedType.isInstance(exe)) {
            executor.triggerMishap(operationName + " 期望:" + targetId +
                    " 类型，实际为：" + StackTools.getId(exe));
            return null;
        }
        return expectedType.cast(exe);
    }

    /**
     * 弹出 NumberData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return NumberData，失败返回 null
     */
    public static NumberData popNumber(StateMachine executor, String operationName) {
        return popAsType(executor, NumberData.class, operationName, "relay:number");
    }

    /**
     * 窥视栈顶 NumberData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return NumberData，失败返回 null
     */
    public static NumberData peekNumber(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, NumberData.class, operationName, "relay:number");
    }

    /**
     * 弹出 BooleanData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BooleanData，失败返回 null
     */
    public static BooleanData popBoolean(StateMachine executor, String operationName) {
        return popAsType(executor, BooleanData.class, operationName, "relay:boolean");
    }

    /**
     * 窥视栈顶 BooleanData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BooleanData，失败返回 null
     */
    public static BooleanData peekBoolean(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, BooleanData.class, operationName, "relay:boolean");
    }

    /**
     * 弹出 VectorData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return VectorData，失败返回 null
     */
    public static VectorData popVector(StateMachine executor, String operationName) {
        return popAsType(executor, VectorData.class, operationName, "relay:vector");
    }

    /**
     * 窥视栈顶 VectorData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return VectorData，失败返回 null
     */
    public static VectorData peekVector(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, VectorData.class, operationName, "relay:vector");
    }

    /**
     * 弹出 EntityData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return EntityData，失败返回 null
     */
    public static EntityData popEntity(StateMachine executor, String operationName) {
        return popAsType(executor, EntityData.class, operationName, "relay:entity");
    }

    /**
     * 窥视栈顶 EntityData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return EntityData，失败返回 null
     */
    public static EntityData peekEntity(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, EntityData.class, operationName, "relay:entity");
    }

    /**
     * 弹出 BlockEntityData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BlockEntityData，失败返回 null
     */
    public static BlockEntityData popBlockEntity(StateMachine executor, String operationName) {
        return popAsType(executor, BlockEntityData.class, operationName, "relay:block_entity");
    }

    /**
     * 窥视栈顶 BlockEntityData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return BlockEntityData，失败返回 null
     */
    public static BlockEntityData peekBlockEntity(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, BlockEntityData.class, operationName, "relay:block_entity");
    }

    /**
     * 弹出 ListData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return ListData，失败返回 null
     */
    public static ListData popList(StateMachine executor, String operationName) {
        return popAsType(executor, ListData.class, operationName, "relay:list");
    }

    /**
     * 窥视栈顶 ListData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return ListData，失败返回 null
     */
    public static ListData peekList(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, ListData.class, operationName, "relay:list");
    }

    /**
     * 弹出 StringData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return StringData，失败返回 null
     */
    public static StringData popString(StateMachine executor, String operationName) {
        return popAsType(executor, StringData.class, operationName, "relay:string");
    }

    /**
     * 窥视栈顶 StringData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return StringData，失败返回 null
     */
    public static StringData peekString(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, StringData.class, operationName, "relay:string");
    }

    /**
     * 弹出 TypeData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return TypeData，失败返回 null
     */
    public static TypeData popType(StateMachine executor, String operationName) {
        return popAsType(executor, TypeData.class, operationName, "relay:type");
    }

    /**
     * 窥视栈顶 TypeData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return TypeData，失败返回 null
     */
    public static TypeData peekType(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, TypeData.class, operationName, "relay:type");
    }

    /**
     * 弹出 TypeData 类型
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return TypeData，失败返回 null
     */
    public static SlotData popSlot(StateMachine executor, String operationName) {
        return popAsType(executor, SlotData.class, operationName, "relay:slot");
    }

    /**
     * 窥视栈顶 TypeData 类型（不弹出）
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @return TypeData，失败返回 null
     */
    public static SlotData peekSlot(StateMachine executor, String operationName) {
        return peekAsType(executor, 0, SlotData.class, operationName, "relay:slot");
    }

    // ==================== 便捷转换方法 ====================

    /**
     * 将 Executable 转换为 NumberData 并获取 double 值
     * 
     * @param exe Executable
     * @return double 值
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
     * @return boolean 值
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
     * @return Vec3 值
     */
    public static net.minecraft.world.phys.Vec3 asVector(Executable exe) {
        if (exe instanceof VectorData vec) {
            return vec.asVector();
        }
        return net.minecraft.world.phys.Vec3.ZERO;
    }

    // ==================== 栈操作便捷方法 ====================

    /**
     * 从数据栈弹出指定数量的参数
     * 
     * @param executor 状态机
     * @param count    参数数量
     * @return Executable 数组，如果栈不足返回 null
     */
    public static Executable[] popMultiple(StateMachine executor, int count) {
        Executable[] result = new Executable[count];
        for (int i = count - 1; i >= 0; i--) {
            result[i] = executor.popData();
            if (result[i] == null) {
                executor.triggerMishap("数据栈不足，需要 " + count + " 个参数");
                return null;
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
     * @return 如果足够返回 true，否则触发事故并返回 false
     */
    public static boolean checkStackSize(StateMachine executor, int required, String operationName) {
        if (executor.getDataStackSize() < required) {
            executor.triggerMishap(operationName + " 需要 " + required + " 个参数");
            return false;
        }
        return true;
    }

    // ==================== 栈索引访问相关 ====================

    /**
     * 从数据栈获取指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 元素，如果失败返回 null
     */
    public static Executable getDataAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return null;
        }
        if (index >= executor.getDataStackSize()) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return null;
        }
        Executable target = executor.getDataAt(index);
        if (target == null) {
            executor.triggerMishap(operationName + " 无法获取目标元素");
        }
        return target;
    }

    /**
     * 从数据栈移除指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 被移除的元素，如果失败返回 null
     */
    public static Executable removeDataAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return null;
        }
        if (index >= executor.getDataStackSize()) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return null;
        }
        Executable target = executor.removeDataAt(index);
        if (target == null) {
            executor.triggerMishap(operationName + " 无法移除目标元素");
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
     * @return 如果成功返回 true，否则返回 false
     */
    public static boolean setDataAt(StateMachine executor, int index, Executable value, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return false;
        }
        if (index >= executor.getDataStackSize()) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return false;
        }
        if (!executor.setDataAt(index, value)) {
            executor.triggerMishap(operationName + " 无法设置目标元素");
            return false;
        }
        return true;
    }

    /**
     * 从程序栈获取指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 元素，如果失败返回 null
     */
    public static Executable getProgramAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return null;
        }
        if (index >= executor.getProgramStackSize()) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return null;
        }
        Executable target = executor.getProgramAt(index);
        if (target == null) {
            executor.triggerMishap(operationName + " 无法获取目标元素");
        }
        return target;
    }

    /**
     * 从程序栈移除指定索引的元素
     *
     * @param executor      状态机
     * @param index         索引（0 为栈顶）
     * @param operationName 操作名称
     * @return 被移除的元素，如果失败返回 null
     */
    public static Executable removeProgramAt(StateMachine executor, int index, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return null;
        }
        if (index >= executor.getProgramStackSize()) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return null;
        }
        Executable target = executor.removeProgramAt(index);
        if (target == null) {
            executor.triggerMishap(operationName + " 无法移除目标元素");
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
     * @return 如果成功返回 true，否则返回 false
     */
    public static boolean setProgramAt(StateMachine executor, int index, Executable value, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return false;
        }
        if (index >= executor.getProgramStackSize()) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return false;
        }
        if (!executor.setProgramAt(index, value)) {
            executor.triggerMishap(operationName + " 无法设置目标元素");
            return false;
        }
        return true;
    }

    /**
     * 验证索引是否有效
     *
     * @param executor      状态机
     * @param index         索引值
     * @param stackSize     栈大小
     * @param operationName 操作名称
     * @return 如果有效返回 true，否则返回 false
     */
    public static boolean checkIndex(StateMachine executor, int index, int stackSize, String operationName) {
        if (index < 0) {
            executor.triggerMishap(operationName + " 索引不能为负数");
            return false;
        }
        if (index >= stackSize) {
            executor.triggerMishap(operationName + " 索引超出栈范围");
            return false;
        }
        return true;
    }
}
