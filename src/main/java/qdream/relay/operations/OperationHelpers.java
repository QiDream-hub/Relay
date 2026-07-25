package qdream.relay.operations;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.component.WorldInteractorComponent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 操作工具类 - 提供世界交互器检查和上下文获取功能
 *
 * <p>
 * 提供以下功能：
 * </p>
 * <ul>
 * <li>世界交互器检查与范围验证</li>
 * <li>能量检查与扣除</li>
 * <li>上下文获取（Level, ShellContainer, Entity, BlockEntity）</li>
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
        // 扣除世界交互器的能量消耗
        ItemStack interactorStack = container.getWorldInteractorStack();
        if (interactorStack.getItem() instanceof WorldInteractorComponent component) {
            double energyCost = component.getEnergyCost(interactorStack);
            if (!container.consumeEnergy(energyCost)) {
                executor.triggerMishap(operationName + " 能量不足：需要 " + energyCost);
                return false;
            }
            container.getExecutionStats().addWorldInteractorEnergy(energyCost);
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

        if (!container.consumeEnergy(dynamicEnergy)) {
            executor.triggerMishap(operationName + " 能量不足：需要 " + dynamicEnergy);
            return false;
        }

        // 记录能量消耗
        container.getExecutionStats().addOperationEnergy(dynamicEnergy);

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
        return Optional.of(container.getWorldInteractorStack());
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
     * 获取所属者引用
     *
     * @param executor 状态机
     * @return owner 所属者引用 只返回玩家,其余返回null
     */
    public static Player getOwner(StateMachine executor) {
        ShellContainer shellContainer = getShellContainer(executor);
        Entity owner = shellContainer.getOwner();
        if (owner instanceof Player player) {
            return player;
        }
        return null;
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
}
