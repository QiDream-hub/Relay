package qdream.relay.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import qdream.relay.engine.StateMachine;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.screen.ShellScreenHandler;

/**
 * 实体外壳
 * 右键召唤，存在时运行程序，表现为粒子效果
 * 注意：26.1.2 API 变化较大，这里暂时简化实现为抽象类
 * 
 * 由于 26.1.2 的 Entity 类有 addAdditionalSaveData(ValueOutput) 抽象方法，
 * 这个类需要保持 abstract，让具体实现类去处理 NBT 持久化
 */
public abstract class EntityShell extends Entity implements MenuProvider, ShellContainer {

    private final ItemStack[] inventory = new ItemStack[4];
    private final StateMachine stateMachine;
    private final ShellTickHandler tickHandler;

    private int energy;
    private int lifetime;
    private int maxLifetime;

    public EntityShell(EntityType<? extends EntityShell> type, Level world) {
        super(type, world);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        this.stateMachine = new StateMachine(1024);
        this.tickHandler = new ShellTickHandler();
        this.energy = 0;
        this.lifetime = 0;
        this.maxLifetime = 6000;

        this.stateMachine.setMishapHandler(reason -> {
            spawnMishapParticles();
        });
    }

    public EntityShell(Level world, double x, double y, double z) {
        this(RelayEntityTypes.SIMPLE_ENTITY_SHELL, world);
        this.setPos(x, y, z);
    }

    // 26.1.2 API 变化，移除 @Override
    protected void onSyncedDataUpdated() {
        // 无需同步数据
    }

    // tick 方法
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            spawnParticles();
            return;
        }

        tickHandler.tick(this);

        lifetime++;
        if (lifetime >= maxLifetime) {
            discard();
        }
    }

    private void spawnParticles() {
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = (random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;
                
                level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    getX() + offsetX,
                    getY() + offsetY,
                    getZ() + offsetZ,
                    0, 0, 0
                );
            }
        }
    }

    private void spawnMishapParticles() {
        if (level().isClientSide()) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 1.0;
                double offsetY = (random.nextDouble() - 0.5) * 1.0;
                double offsetZ = (random.nextDouble() - 0.5) * 1.0;
                
                level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                    getX() + offsetX,
                    getY() + offsetY,
                    getZ() + offsetZ,
                    0, 0, 0
                );
            }
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    // ========== NBT 持久化 - 简化实现 ==========

    // 26.1.2 使用 ValueOutput/ValueInput，CompoundTag 方法不再使用
    // 这些方法保留用于向后兼容，但实际持久化在 addAdditionalSaveData/readAdditionalSaveData 中处理
    protected void saveWithoutId(CompoundTag tag) {
        tag.putInt("lifetime", lifetime);
        tag.putInt("energy", energy);
    }

    protected void loadAdditionalSaveData(CompoundTag tag) {
        lifetime = tag.getInt("lifetime").orElse(0);
        energy = tag.getInt("energy").orElse(0);
    }

    // ========== MenuProvider 接口 ==========

    public Component getDisplayName() {
        return Component.literal("实体外壳");
    }

    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        // 传递 this（ShellContainer）作为 blockEntity 参数
        return new ShellScreenHandler(syncId, inv, this);
    }

    // ========== 网络同步 ==========

    // 26.1.2 API - 覆盖此方法提供实体生成包
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    // ========== ShellContainer 接口 ==========

    public ItemStack getInventorySlot(int slot) {
        if (slot >= 0 && slot < inventory.length) {
            return inventory[slot];
        }
        return ItemStack.EMPTY;
    }

    public void setInventorySlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.length) {
            inventory[slot] = stack;
        }
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public int getCoreCount() {
        return tickHandler.getCoreCount();
    }

    public int getInterval() {
        return tickHandler.getInterval();
    }

    public boolean isInitialized() {
        return tickHandler.isInitialized();
    }

    public void setInitialized(boolean initialized) {
        tickHandler.setInitialized(initialized);
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public void setChanged() {
        // 实体需要标记为脏数据以进行保存
    }

    public boolean isClientSide() {
        return level().isClientSide();
    }

    // ========== 状态访问 ==========

    public int getLifetime() {
        return lifetime;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }
}
