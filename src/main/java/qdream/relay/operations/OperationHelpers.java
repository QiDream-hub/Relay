package qdream.relay.operations;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.container.ContainerContextMissingError;
import qdream.relay.mc.errors.container.ContainerException;
import qdream.relay.mc.errors.energy.EnergyException;
import qdream.relay.mc.errors.energy.EnergyInsufficientError;
import qdream.relay.mc.errors.entity.OwnerNotFoundError;
import qdream.relay.mc.errors.world.WorldInteractionException;
import qdream.relay.mc.errors.world.WorldInteractorMissingError;
import qdream.relay.mc.errors.world.WorldInteractorOutOfRangeError;
import qdream.relay.mc.errors.world.WorldNotAvailableError;
import qdream.relay.tools.TextTools;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

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
 *
 * <p>
 * <b>上下文获取约定：</b>外壳容器、世界、所属者都是操作执行的前提，缺失说明执行环境不完整，
 * 因此这些获取方法直接抛出对应的事故（{@link ContainerContextMissingError}、
 * {@link WorldNotAvailableError}、{@link OwnerNotFoundError} 等），返回值保证非空。
 * 唯一的例外是 {@link #getSelf(StateMachine)}：自身引用允许不存在，此时返回 {@code null}。
 * </p>
 */
public final class OperationHelpers {

    private OperationHelpers() {
        // 防止实例化
    }

    // ==================== 世界交互器相关 ====================

    /**
     * 从状态机获取 ShellContainer
     * <p>
     * 外壳容器是操作执行的必需上下文，缺失说明执行环境不完整，此时抛出
     * {@link ContainerContextMissingError}。
     * </p>
     *
     * @param executor 状态机
     * @return ShellContainer，保证非 {@code null}
     * @throws ContainerContextMissingError 如果状态机中没有外壳容器上下文
     */
    @NotNull
    public static ShellContainer getShellContainer(StateMachine executor) {
        return executor.getContext("shellContainer", ShellContainer.class)
                .orElseThrow(() -> new ContainerContextMissingError(executor));
    }

    /**
     * 检查世界交互器是否存在
     *
     * @param executor      状态机
     * @param operationName 操作名称（用于错误消息）
     * @throws ContainerContextMissingError 如果无法获取容器上下文
     * @throws WorldInteractionException    如果世界交互器不存在
     * @throws EnergyException              如果能量不足
     */
    public static void checkWorldInteractor(StateMachine executor, String operationName) {
        ShellContainer container = getShellContainer(executor);
        if (!container.hasWorldInteractor()) {
            throw new WorldInteractorMissingError(executor, TextTools.getName(operationName).withColor(0xFF5555));
        }
        // 扣除世界交互器的能量消耗
        double energyCost = container.getWorldInteractorEnergyCost();
        if (!container.consumeEnergy(energyCost)) {
            throw new EnergyInsufficientError(executor, energyCost);
        }
        container.getExecutionStats().addWorldInteractorEnergy(energyCost);
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
     * @throws ContainerException 如果无法获取容器上下文
     * @throws EnergyException    如果能量不足
     */
    public static void checkEnergy(StateMachine executor, String operationName, double dynamicEnergy) {
        ShellContainer container = getShellContainer(executor);

        if (!container.consumeEnergy(dynamicEnergy)) {
            throw new EnergyInsufficientError(executor, TextTools.getName(operationName).withColor(0xFF5555),
                    dynamicEnergy);
        }

        // 记录能量消耗
        container.getExecutionStats().addOperationEnergy(dynamicEnergy);
    }

    /**
     * 检查目标位置是否在世界交互器范围内
     *
     * <p>
     * 此方法会检查世界交互器是否存在，如果不存在会触发事故。
     * </p>
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @param sourcePos     源位置
     * @param targetPos     目标位置
     * @throws ContainerContextMissingError 如果无法获取容器上下文
     * @throws WorldInteractionException    如果世界交互器不存在或超出范围
     */
    public static void checkInRange(StateMachine executor, String operationName,
            Vec3 sourcePos,
            Vec3 targetPos) {

        ShellContainer container = getShellContainer(executor);
        if (!container.isWorldInRange(sourcePos, targetPos)) {
            double distance = sourcePos.distanceTo(targetPos);
            double range = container.getWorldInteractorRange();
            throw new WorldInteractorOutOfRangeError(executor, TextTools.getName(operationName).withColor(0xFF5555),
                    distance, range);
        }
    }

    /**
     * 检查球体区域是否与世界交互器范围有交集
     *
     * <p>
     * 用于扫描类操作，检查以 center 为中心、radius 为半径的球体
     * 是否与世界交互器的作用范围有交集。
     * 条件：distance(center, sourcePos) - radius ≤ range
     * </p>
     *
     * @param executor      状态机
     * @param operationName 操作名称
     * @param sourcePos     源位置（世界交互器位置）
     * @param center        球体中心
     * @param radius        球体半径
     * @throws ContainerContextMissingError 如果无法获取容器上下文
     * @throws WorldInteractionException    如果球体与世界交互器范围无交集
     */
    public static void checkSphereInRange(StateMachine executor, String operationName,
            Vec3 sourcePos,
            Vec3 center,
            double radius) {

        ShellContainer container = getShellContainer(executor);
        double distance = sourcePos.distanceTo(center);
        double range = container.getWorldInteractorRange();
        // 检查球体最近边缘是否在范围内
        if (distance - radius > range) {
            throw new WorldInteractorOutOfRangeError(executor, TextTools.getName(operationName).withColor(0xFF5555),
                    distance - radius, range);
        }
    }

    // ==================== 上下文获取 ====================

    /**
     * 从状态机获取 Level 上下文
     *
     * @param executor      状态机
     * @param operationName 操作名称（用于错误消息）
     * @return Level
     * @throws WorldInteractionException 如果无法获取世界
     */
    public static Level getLevel(StateMachine executor, String operationName) {
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty() || levelOpt.get() == null) {
            throw new WorldNotAvailableError(executor, TextTools.getName(operationName).withColor(0xFF5555));
        }
        return levelOpt.get();
    }

    /**
     * 获取执行者自身引用（Entity 或 BlockEntity）
     * <p>
     * 这是本类中唯一允许返回 {@code null} 的上下文获取方法：自身引用可能不存在，
     * 调用方需要自行处理（例如 {@link #getSelfPosition(StateMachine)} 会退化为原点）。
     * </p>
     *
     * @param executor 状态机
     * @return self 引用，可能为 Entity、BlockEntity 或 {@code null}
     */
    public static Object getSelf(StateMachine executor) {
        return executor.getContext("self", Object.class).orElse(null);
    }

    /**
     * 获取所属者引用
     *
     * @param executor 状态机
     * @return owner 所属者引用 只返回玩家
     * @throws ContainerContextMissingError 如果无法获取容器上下文
     * @throws OwnerNotFoundError           如果无法获取所属者
     */
    public static Player getOwner(StateMachine executor) {
        Player owner = getShellContainer(executor).getOwner();
        if (owner == null) {
            throw new OwnerNotFoundError(executor);
        }
        return owner;
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

    // ==================== 能量管理相关 ====================

    /**
     * 检查是否有足够能量（不扣除）
     *
     * @param executor 状态机
     * @param amount   需要的能量值
     * @return 如果能量充足返回 true
     * @throws ContainerException 如果无法获取容器上下文
     */
    public static boolean hasEnoughEnergy(StateMachine executor, double amount) {
        return getShellContainer(executor).getEnergy() >= amount;
    }

    /**
     * 获取可用能量
     *
     * @param executor 状态机
     * @return 当前可用能量
     * @throws ContainerException 如果无法获取容器上下文
     */
    public static double getAvailableEnergy(StateMachine executor) {
        return getShellContainer(executor).getEnergy();
    }

    /**
     * 消耗能量
     *
     * @param executor 状态机
     * @param amount   消耗的能量值
     * @return 如果成功扣除返回 true
     * @throws ContainerException 如果无法获取容器上下文
     */
    public static boolean consumeEnergy(StateMachine executor, double amount) {
        return getShellContainer(executor).consumeEnergy(amount);
    }

    /**
     * 添加能量（返还能量时使用）
     *
     * @param executor 状态机
     * @param amount   添加的能量值
     * @return 实际添加的能量值
     * @throws ContainerException 如果无法获取容器上下文
     */
    public static double addEnergy(StateMachine executor, double amount) {
        return getShellContainer(executor).addEnergy(amount);
    }

    // ==================== 工具方法相关 ====================

    /**
     * 从向量转换为方向
     * 找出绝对值最大的分量作为主方向
     *
     * @param vec 方向向量
     * @return 最接近的 Direction
     */
    public static Direction getDirectionFromVector(Vec3 vec) {
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;

        // 找出绝对值最大的分量
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);

        if (absY > absX && absY > absZ) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        } else if (absZ > absX && absZ > absY) {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        } else {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
    }
}
