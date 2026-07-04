package qdream.relay.operations;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.types.NumberData;
import qdream.relay.types.BooleanData;
import qdream.relay.types.VectorData;
import qdream.relay.types.EntityData;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.StringData;
import qdream.relay.types.TypeData;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * 操作工具类 - 提取操作中重复使用的公共逻辑
 * 
 * <p>提供以下功能：</p>
 * <ul>
 *   <li>世界交互器检查与范围验证</li>
 *   <li>上下文获取（Level, ShellContainer, Entity, BlockEntity）</li>
 *   <li>类型安全的栈弹出与类型检查</li>
 *   <li>常见类型转换</li>
 * </ul>
 */
public final class OperationHelpers {
    
    private OperationHelpers() {
        // 防止实例化
    }
    
    // ==================== 世界交互器相关 ====================
    
    /**
     * 从状态机获取 ShellContainer
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
     * @param executor 状态机
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
     * 获取世界交互器物品栈
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
     * @param executor 状态机
     * @param operationName 操作名称
     * @param sourcePos 源位置
     * @param targetPos 目标位置
     * @return 如果在范围内返回 true，否则返回 false
     */
    public static boolean checkInRange(StateMachine executor, String operationName,
                                       net.minecraft.world.phys.Vec3 sourcePos,
                                       net.minecraft.world.phys.Vec3 targetPos) {
        Optional<ItemStack> interactorOpt = getWorldInteractorStack(executor);
        if (interactorOpt.isEmpty()) {
            executor.triggerMishap(operationName + " 需要世界交互器");
            return false;
        }
        ItemStack interactor = interactorOpt.get();
        if (!(interactor.getItem() instanceof qdream.relay.mc.component.WorldInteractorComponent component)) {
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
     * @param executor 状态机
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
     * @param executor 状态机
     * @return self 引用，可能为 Entity、BlockEntity 或 null
     */
    public static Object getSelf(StateMachine executor) {
        return executor.getContext("self", Object.class).orElse(null);
    }
    
    /**
     * 获取执行者自身位置
     * @param executor 状态机
     * @return 自身位置，如果无法获取返回 (0,0,0)
     */
    public static net.minecraft.world.phys.Vec3 getSelfPosition(StateMachine executor) {
        Object self = getSelf(executor);
        if (self instanceof Entity entity) {
            return entity.position();
        } else if (self instanceof BlockEntity blockEntity) {
            net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
            return net.minecraft.world.phys.Vec3.atCenterOf(pos);
        }
        return new net.minecraft.world.phys.Vec3(0, 0, 0);
    }
    
    // ==================== 类型安全的栈弹出 ====================
    
    /**
     * 从数据栈弹出并检查类型
     * @param executor 状态机
     * @param expectedType 期望的类型
     * @param operationName 操作名称（用于错误消息）
     * @param <T> 期望的类型
     * @return 转换后的值，如果失败触发事故并返回 null
     */
    public static <T> T popAsType(StateMachine executor, Class<T> expectedType, String operationName) {
        Executable exe = executor.popData();
        if (exe == null) {
            executor.triggerMishap(operationName + " 数据栈不足");
            return null;
        }
        if (!expectedType.isInstance(exe)) {
            executor.triggerMishap(operationName + " 期望 " + expectedType.getSimpleName() + 
                                  " 类型，实际为：" + exe.getClass().getSimpleName());
            return null;
        }
        return expectedType.cast(exe);
    }
    
    /**
     * 弹出 NumberData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return NumberData，失败返回 null
     */
    public static NumberData popNumber(StateMachine executor, String operationName) {
        return popAsType(executor, NumberData.class, operationName);
    }
    
    /**
     * 弹出 BooleanData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return BooleanData，失败返回 null
     */
    public static BooleanData popBoolean(StateMachine executor, String operationName) {
        return popAsType(executor, BooleanData.class, operationName);
    }
    
    /**
     * 弹出 VectorData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return VectorData，失败返回 null
     */
    public static VectorData popVector(StateMachine executor, String operationName) {
        return popAsType(executor, VectorData.class, operationName);
    }
    
    /**
     * 弹出 EntityData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return EntityData，失败返回 null
     */
    public static EntityData popEntity(StateMachine executor, String operationName) {
        return popAsType(executor, EntityData.class, operationName);
    }
    
    /**
     * 弹出 BlockEntityData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return BlockEntityData，失败返回 null
     */
    public static BlockEntityData popBlockEntity(StateMachine executor, String operationName) {
        return popAsType(executor, BlockEntityData.class, operationName);
    }
    
    /**
     * 弹出 ListData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return ListData，失败返回 null
     */
    public static ListData popList(StateMachine executor, String operationName) {
        return popAsType(executor, ListData.class, operationName);
    }
    
    /**
     * 弹出 StringData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return StringData，失败返回 null
     */
    public static StringData popString(StateMachine executor, String operationName) {
        return popAsType(executor, StringData.class, operationName);
    }
    
    /**
     * 弹出 TypeData 类型
     * @param executor 状态机
     * @param operationName 操作名称
     * @return TypeData，失败返回 null
     */
    public static TypeData popType(StateMachine executor, String operationName) {
        return popAsType(executor, TypeData.class, operationName);
    }
    
    // ==================== 便捷转换方法 ====================
    
    /**
     * 将 Executable 转换为 NumberData 并获取 double 值
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
     * @param executor 状态机
     * @param count 参数数量
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
     * @param executor 状态机
     * @param required 需要的元素数量
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
}
