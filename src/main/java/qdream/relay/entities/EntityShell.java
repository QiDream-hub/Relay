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
import net.minecraft.core.particles.ParticleTypes;

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
    private static final EntityDataAccessor<Float> DATA_ENERGY = SynchedEntityData.defineId(EntityShell.class,
            EntityDataSerializers.FLOAT);

    public EntityShell(EntityType<?> type, Level level) {
        super(type, level);
        this.stateMachine = new StateMachine(Relay.DEFAULT_MAX_PROGRAM_STACK_SIZE);

        // 设置事故回调
        stateMachine.setMishapHandler(reason -> {
            if (!level().isClientSide()) {
                getOwner().sendSystemMessage(Component.literal(String.format("§c§lMISHAP§r§c[实体]: %s", reason)));
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ENABLED, true);
        builder.define(DATA_ENERGY, 0.0f);
    }

    // ========== Tick 逻辑 ==========

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            spawnParticles();
            return;
        }

        // 服务端：执行 tick 逻辑
        var machine = getStateMachine();
        if (machine.isRunning()) {
            machine.setContext("level", level());
            machine.setContext("self", this);
        }
        // 执行前检查是否还有能量
        if (getEnergy() < getEnergyCostPerTick()) {
            machine.clear();
            discard();
        }
        tickHandler.tick(this);

        // 服务端：每 5 tick 同步一次能量到客户端（使用同步数据字段）
        if (level().getGameTime() % 5 == 0) {
            entityData.set(DATA_ENERGY, (float) getEnergy());
        }
    }

    /**
     * 生成粒子效果（仅在客户端调用）
     * <p>
     * 粒子特征：
     * - 白色粒子 (END_ROD): 每 10 能量 1 个，表示基础能量
     * - 金色粒子 (GLOW): 每 100 能量 1 个，表示高能量
     * - 粒子在实体周围的随机范围内生成，向上飘散
     * - 本体位置始终有 1 个静止粒子
     * </p>
     */
    private void spawnParticles() {
        // 从同步数据字段读取能量值（避免客户端本地值为 0）
        double energy = entityData.get(DATA_ENERGY);

        // 粒子数量上限
        int maxWhiteParticles = 10;
        int maxGoldParticles = 20;

        double x = getX();
        double y = getY();
        double z = getZ();

        var random = level().getRandom();

        int goldCount = Math.min((int) (energy / 1000.0), maxGoldParticles);
        // 每 5 tick 生成一次粒子效果（避免粒子重叠）
        if (level().getGameTime() % 16 == 0) {
            for (int i = 0; i < goldCount; i++) {
                // 随机范围：X/Z 方向 ±0.4，Y 方向 [y-0.6, y+0.4]
                double offsetX = random.nextFloat() * 0.8 - 0.4;
                double offsetY = random.nextFloat() * 1.0 - 0.6 + 0.5;
                double offsetZ = random.nextFloat() * 0.8 - 0.4;

                // 更快的向上速度
                double velX = (random.nextFloat() - 0.5) * 0.08;
                double velY = 0.06 + random.nextFloat() * 0.08;
                double velZ = (random.nextFloat() - 0.5) * 0.08;

                level().addParticle(
                        ParticleTypes.GLOW,
                        x + offsetX,
                        y + offsetY,
                        z + offsetZ,
                        velX, velY, velZ);
            }
        }

        if (level().getGameTime() % 5 == 0) {
            double remainingEnergy = energy - (goldCount * 1000.0);
            int whiteCount = Math.min((int) (remainingEnergy / 100.0), maxWhiteParticles);
            for (int i = 0; i < whiteCount; i++) {
                // 随机范围：X/Z 方向 ±0.6，Y 方向 [y-0.8, y+0.2]
                double offsetX = random.nextFloat() * 1.2 - 0.6;
                double offsetY = random.nextFloat() * 1.0 - 0.8 + 0.8;
                double offsetZ = random.nextFloat() * 1.2 - 0.6;

                // 向上飘散
                double velX = (random.nextFloat() - 0.5) * 0.05;
                double velY = 0.03 + random.nextFloat() * 0.05;
                double velZ = (random.nextFloat() - 0.5) * 0.05;

                level().addParticle(
                        ParticleTypes.ENCHANT,
                        x + offsetX,
                        y + offsetY,
                        z + offsetZ,
                        velX, velY, velZ);
            }
        }

        // 本体位置始终有 1 个静止粒子（表示实体存在）
        if (level().getGameTime() % 10 == 0) {
            level().addParticle(
                    ParticleTypes.END_ROD,
                    x,
                    y + 0.5,
                    z,
                    0.0, 0.0, 0.0);
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
        return (1.0 / interval) * Math.pow(coreCost, 1.3);
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
        return this.energy;
    }

    @Override
    public void setEnergy(double energy) {
        this.energy = energy;
    }

    @Override
    public boolean consumeEnergy(double amount) {
        if (this.energy >= amount) {
            this.energy -= amount;
            return true;
        }
        return false;
    }

    @Override
    public double addEnergy(double amount) {
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
        // 保存配置到统一的 CompoundTag
        CompoundTag configTag = new CompoundTag();
        configTag.putInt("CoreCost", coreCost);
        configTag.putInt("Interval", interval);
        configTag.putInt("Range", range);
        configTag.putDouble("Energy", energy);
        output.store("Config", CompoundTag.CODEC, configTag);

        // 保存所有者
        if (ownerUuid != null) {
            output.putString("OwnerUUID", ownerUuid.toString());
        }

        // 保存状态机
        CompoundTag machineTag = qdream.relay.mc.StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        output.store("StateMachine", CompoundTag.CODEC, machineTag);

        // 保存 TickHandler 状态（使用 ShellTickHandler 自己的序列化方法）
        CompoundTag tickHandlerTag = tickHandler.toNbt();
        output.store("TickHandler", CompoundTag.CODEC, tickHandlerTag);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // 加载配置从统一的 CompoundTag
        input.read("Config", CompoundTag.CODEC).ifPresent(configTag -> {
            coreCost = configTag.getInt("CoreCost").orElse(1);
            interval = configTag.getInt("Interval").orElse(20);
            range = configTag.getInt("Range").orElse(32);
            energy = configTag.getDouble("Energy").orElse(0.0);
        });

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

        // 加载 TickHandler 状态（使用 ShellTickHandler 自己的反序列化方法）
        input.read("TickHandler", CompoundTag.CODEC).ifPresent(tag -> {
            tickHandler.fromNbt(tag);
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
