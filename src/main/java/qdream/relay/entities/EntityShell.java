package qdream.relay.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.core.ExecutionStats;
import qdream.relay.engine.StateMachine;
import qdream.relay.Relay;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.component.DiskComponent;

/**
 * 外壳实体
 * 
 * <h3>特性</h3>
 * <ul>
 * <li>实现 ShellContainer 接口，与 BlockShellEntity 一致的接口</li>
 * <li>粒子效果渲染（无实体模型）</li>
 * <li>独立能量池，可手动补充</li>
 * <li>Owner 绑定，可远程终止</li>
 * </ul>
 */
public class EntityShell extends Entity implements ShellContainer {

    private final ShellTickHandler tickHandler = new ShellTickHandler();
    private final StateMachine stateMachine;
    private final ExecutionStats executionStats = new ExecutionStats();

    // 配置属性（召唤时设置）
    private int coreCost = 1;
    private int interval = 20;
    private int range = 32;
    private double energy = 0;

    // Owner 管理
    private Player owner;
    private UUID ownerUuid;

    // 同步数据
    private static final EntityDataAccessor<Boolean> DATA_ENABLED = SynchedEntityData.defineId(EntityShell.class,
            EntityDataSerializers.BOOLEAN);

    public EntityShell(EntityType<?> type, Level level) {
        super(type, level);
        this.stateMachine = new StateMachine(Relay.DEFAULT_MAX_PROGRAM_STACK_SIZE);

        // 设置事故回调
        stateMachine.setMishapHandler(reason -> {
            if (!level().isClientSide()) {
                getOwner().sendSystemMessage(Component.literal("[实体]:" + reason));
                remove(getRemovalReason());
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ENABLED, true);
    }

    // ========== Tick 逻辑 ==========

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        // 设置上下文
        var machine = getStateMachine();
        if (machine.isRunning()) {
            machine.setContext("level", level());
            machine.setContext("self", this);
        }

        // 执行 tick
        tickHandler.tick(this);

        // 粒子效果 - 运行时产生粒子
        if (isRunning()) {
            spawnParticles();
        }
    }

    /**
     * 生成粒子效果
     */
    private void spawnParticles() {
        if (level().isClientSide()) {
            // 客户端生成简单粒子
            for (int i = 0; i < 2; i++) {
                level().addParticle(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        getX() + random.nextFloat() * 1.2 - 0.6,
                        getY() + random.nextFloat() * 1.2 - 0.6,
                        getZ() + random.nextFloat() * 1.2 - 0.6,
                        0, 0, 0);
            }
        }
    }

    // ========== ShellContainer 接口实现 ==========

    @Override
    public ItemStack getCoreStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getDiskStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getEnergyStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getWorldInteractorStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public StateMachine getStateMachine() {
        return stateMachine;
    }

    @Override
    public Player getOwner() {
        if (owner != null) {
            return owner;
        }
        if (ownerUuid != null && !level().isClientSide()) {
            var serverLevel = (net.minecraft.server.level.ServerLevel) level();
            Player player = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
            if (player != null) {
                owner = player;
                return owner;
            }
        }
        return null;
    }

    @Override
    public void setOwner(Player owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
    }

    @Override
    public int getCoreCost() {
        return coreCost;
    }

    /**
     * 设置核心数量（召唤时设置）
     */
    public void setCoreCost(int cost) {
        this.coreCost = cost;
    }

    @Override
    public int getInterval() {
        return interval;
    }

    /**
     * 设置执行间隔（召唤时设置）
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * 设置世界交互器范围（召唤时设置）
     */
    public void setRange(int range) {
        this.range = range;
    }

    @Override
    public double getEnergyCostPerTick() {
        ItemStack coreStack = getCoreStack();
        if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
            return core.getEnergyCost(coreStack);
        }
        return 0.0;
    }

    @Override
    public boolean isInitialized() {
        return tickHandler.isInitialized();
    }

    @Override
    public void setInitialized(boolean initialized) {
        tickHandler.setInitialized(initialized);
    }

    @Override
    public boolean isRunning() {
        return getStateMachine().isRunning();
    }

    @Override
    public boolean canExecute() {
        return isInitialized() && isRunning();
    }

    @Override
    public double getEnergy() {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            return emi.getStoredEnergy(energyStack);
        }
        return energy;
    }

    @Override
    public void setEnergy(double energy) {
        this.energy = energy;
    }

    @Override
    public boolean consumeEnergy(double amount) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            double consumed = emi.consumeEnergy(energyStack, amount);
            return consumed >= amount;
        }
        // 从内部能量池扣除
        if (this.energy >= amount) {
            this.energy -= amount;
            return true;
        }
        return false;
    }

    @Override
    public double addEnergy(double amount) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            return emi.addEnergy(energyStack, amount);
        }
        // 添加到内部能量池
        this.energy += amount;
        return amount;
    }

    @Override
    public boolean hasWorldInteractor() {
        return true;
    }

    // 由于没有物品直接直接使用实体的 range 属性进行判断
    @Override
    public boolean isWorldInRange(Vec3 sourcePos, Vec3 targetPos) {
        double distance = sourcePos.distanceTo(targetPos);
        return distance <= range;
    }

    @Override
    public double getWorldInteractorEnergyCost() {
        // 根据范围的对数计算能量消耗，范围越大消耗越高
        return Math.log1p(range) * 0.5;
    }

    @Override
    public double getWorldInteractorRange() {
        return range;
    }

    @Override
    public void loadProgramFromDisk() {
        if (level().isClientSide()) {
            return;
        }

        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty()) {
            return;
        }

        DiskComponent diskComponent = (DiskComponent) diskStack.getItem();

        getStateMachine().clear();
        var program = diskComponent.getProgram(diskStack);
        if (!program.isEmpty()) {
            getStateMachine().loadProgram(program);
            setInitialized(true);
        }
    }

    @Override
    public ExecutionStats getExecutionStats() {
        return executionStats;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    @Override
    public boolean hasOwner() {
        return owner != null || ownerUuid != null;
    }

    // ========== 实体 NBT 序列化（参考 ItemEntity） ==========

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

        // 保存配置
        output.putInt("CoreCost", coreCost);
        output.putInt("Interval", interval);
        output.putInt("Range", range);
        output.putDouble("Energy", energy);

        // 保存所有者
        if (ownerUuid != null) {
            output.putString("OwnerUUID", ownerUuid.toString());
        }

        // 保存状态机
        CompoundTag machineTag = qdream.relay.mc.StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        output.store("StateMachine", CompoundTag.CODEC, machineTag);

        // 保存执行统计
        CompoundTag statsTag = executionStats.toNbt();
        output.store("ExecutionStats", CompoundTag.CODEC, statsTag);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

        // 加载配置
        coreCost = input.getIntOr("CoreCost", 1);
        interval = input.getIntOr("Interval", 20);
        range = input.getIntOr("Range", 32);
        energy = input.getDoubleOr("Energy", 0.0);

        // 加载所有者
        String uuidStr = input.getString("OwnerUUID").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUuid = java.util.UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                // 忽略
            }
        }

        // 加载状态机
        input.read("StateMachine", CompoundTag.CODEC).ifPresent(tag -> {
            qdream.relay.mc.StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, tag);
        });

        // 加载执行统计
        input.read("ExecutionStats", CompoundTag.CODEC).ifPresent(tag -> {
            executionStats.fromNbt(tag);
        });
    }

    // ========== 战斗相关（必需实现） ==========

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Shell 实体不受伤害
        return false;
    }

    // ========== 碰撞箱 ==========

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ========== 物品栏接口（Container） ==========

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public boolean stillValid(Player player) {
        return this.isAlive();
    }

    @Override
    public void setChanged() {
    }

}
