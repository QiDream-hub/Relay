package qdream.relay.core;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import qdream.relay.engine.StateMachine;

/**
 * 外壳容器接口
 * 三种外壳（方块/实体/工具）共享的统一接口
 *
 * <p>这是 Shell 的权威状态接口，所有状态调整都应通过此接口进行：</p>
 * <ul>
 *   <li>能量管理：{@link #getEnergy()}, {@link #addEnergy(double)}, {@link #consumeEnergy(double)}</li>
 *   <li>核心状态：{@link #getCoreCost()}, {@link #getInterval()}, {@link #getEnergyCostPerTick()}</li>
 *   <li>程序控制：{@link #loadProgramFromDisk()}</li>
 *   <li>运行状态：{@link #isEnabled()}, {@link #isInitialized()}, {@link #isRunning()}</li>
 * </ul>
 *
 * <p>实现类不应直接访问内部物品（如能量模块、核心），所有访问都通过接口方法封装。</p>
 *
 * <h3>与 Container 接口的关系</h3>
 * <p>{@code ShellContainer} 继承自 {@code Container}，物品栏访问直接通过父接口方法。</p>
 */
public interface ShellContainer extends Container {

    // ========== 物品栏访问（Container 接口方法） ==========

    /**
     * 获取核心物品（插槽 0）
     */
    default ItemStack getCoreStack() {
        return getItem(0);
    }

    /**
     * 获取法术磁盘（插槽 1）
     */
    default ItemStack getDiskStack() {
        return getItem(1);
    }

    /**
     * 获取能量模块（插槽 2）
     */
    default ItemStack getEnergyStack() {
        return getItem(2);
    }

    /**
     * 获取世界交互器（插槽 3）
     */
    default ItemStack getInteractorStack() {
        return getItem(3);
    }

    // ========== 状态机访问 ==========

    /**
     * 获取状态机
     */
    StateMachine getStateMachine();

    // ========== 所有者（主人）管理 ==========

    /**
     * 获取所有者实体（玩家或其他实体）
     *
     * @return 所有者实体，如果未设置返回 null
     */
    Entity getOwner();

    /**
     * 设置所有者实体
     *
     * @param owner 所有者实体
     */
    void setOwner(Entity owner);

    /**
     * 是否有所有者
     *
     * @return 是否已设置所有者
     */
    boolean hasOwner();

    // ========== 核心状态访问 ==========

    /**
     * 获取核心数量（决定每 tick 最大可执行操作数）
     */
    int getCoreCost();

    /**
     * 获取执行间隔（tick 间隔）
     */
    int getInterval();

    /**
     * 获取每 tick 基础能量消耗（由核心品阶决定）
     */
    double getEnergyCostPerTick();

    // ========== 运行状态 ==========

    /**
     * 是否已初始化（程序已加载且准备执行）
     */
    boolean isInitialized();

    /**
     * 设置初始化状态
     */
    void setInitialized(boolean initialized);

    /**
     * 是否已启用（开关状态）
     */
    boolean isEnabled();

    /**
     * 设置启用状态
     */
    void setEnabled(boolean enabled);

    /**
     * 是否正在运行（状态机程序栈非空）
     */
    default boolean isRunning() {
        return getStateMachine().isRunning();
    }

    // ========== 能量管理 ==========

    /**
     * 获取当前可用能量
     *
     * <p>对于工具外壳，如果启用背包能量模块，返回背包内所有能量模块的总能量。</p>
     */
    double getEnergy();

    /**
     * 设置能量（内部使用）
     *
     * <p>仅用于方块外壳同步内部字段，工具外壳不应调用此方法。</p>
     * @param energy 能量值
     */
    void setEnergy(double energy);

    /**
     * 添加能量
     *
     * @param amount 添加的能量值
     * @return 实际添加的能量值（可能因容量限制而小于输入值）
     */
    double addEnergy(double amount);

    /**
     * 消耗能量
     *
     * @param amount 消耗的能量值
     * @return 如果能量充足并成功扣除返回 true，否则返回 false
     */
    boolean consumeEnergy(double amount);

    /**
     * 是否有足够能量
     *
     * @param amount 需要的能量值
     * @return 如果能量充足返回 true
     */
    default boolean hasEnoughEnergy(double amount) {
        return getEnergy() >= amount;
    }

    // ========== 世界交互 ==========

    /**
     * 是否有世界交互器
     */
    boolean hasWorldInteractor();

    // ========== 程序控制 ==========

    /**
     * 从磁盘重新加载程序
     *
     * <p>清空双栈后从法术磁盘重新加载程序。</p>
     */
    void loadProgramFromDisk();

    // ========== 标记变更 ==========

    /**
     * 标记容器已变更（需要保存）
     */
    @Override
    void setChanged();

    /**
     * 客户端/服务端判断
     */
    boolean isClientSide();
}
