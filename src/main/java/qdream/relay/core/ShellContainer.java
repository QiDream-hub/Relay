package qdream.relay.core;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import qdream.relay.engine.StateMachine;

/**
 * 外壳容器接口
 * 三种外壳（方块/实体/工具）共享的统一接口
 */
public interface ShellContainer {

    // ========== 插槽索引 ==========
    int CORE_SLOT = 0;
    int DISK_SLOT = 1;
    int ENERGY_SLOT = 2;
    int INTERACTOR_SLOT = 3;

    // ========== 物品栏访问 ==========

    /**
     * 获取指定插槽的物品
     */
    ItemStack getInventorySlot(int slot);

    /**
     * 设置指定插槽的物品
     */
    void setInventorySlot(int slot, ItemStack stack);

    /**
     * 获取核心物品
     */
    default ItemStack getCoreStack() {
        return getInventorySlot(CORE_SLOT);
    }

    /**
     * 获取法术磁盘
     */
    default ItemStack getDiskStack() {
        return getInventorySlot(DISK_SLOT);
    }

    /**
     * 获取能量模块
     */
    default ItemStack getEnergyStack() {
        return getInventorySlot(ENERGY_SLOT);
    }

    /**
     * 获取世界交互器
     */
    default ItemStack getInteractorStack() {
        return getInventorySlot(INTERACTOR_SLOT);
    }

    // ========== 状态机访问 ==========

    /**
     * 获取状态机
     */
    StateMachine getStateMachine();

    // ========== 所有者（主人）管理 ==========

    /**
     * 获取所有者实体（玩家或其他实体）
     * @return 所有者实体，如果未设置返回 null
     */
    Entity getOwner();

    /**
     * 设置所有者实体
     * @param owner 所有者实体
     */
    void setOwner(Entity owner);

    /**
     * 是否有所有者
     * @return 是否已设置所有者
     */
    default boolean hasOwner() {
        return getOwner() != null;
    }

    // ========== 状态访问 ==========

    /**
     * 获取核心数量
     */
    int getCoreCount();

    /**
     * 获取执行间隔
     */
    int getInterval();

    /**
     * 是否已初始化
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
     * 获取当前能量
     */
    double getEnergy();

    /**
     * 设置能量
     */
    void setEnergy(double energy);

    /**
     * 是否有世界交互器
     */
    default boolean hasWorldInteractor() {
        return !getInteractorStack().isEmpty();
    }

    // ========== 标记变更 ==========

    /**
     * 标记容器已变更（需要保存）
     */
    void setChanged();

    /**
     * 客户端/服务端判断
     */
    boolean isClientSide();
}
