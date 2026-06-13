package qdream.relay.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * 实体外壳的具体实现
 * 注意：26.1.2 API 变化较大，这里暂时简化实现
 */
public class SimpleEntityShell extends EntityShell {

    public SimpleEntityShell(EntityType<? extends SimpleEntityShell> type, Level world) {
        super(type, world);
    }

    public SimpleEntityShell(Level world, double x, double y, double z) {
        this(RelayEntityTypes.SIMPLE_ENTITY_SHELL, world);
        this.setPos(x, y, z);
    }

    // 26.1.2 新增的抽象方法 - 简化实现
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("lifetime", getLifetime());
        output.putInt("energy", getEnergy());
        output.putBoolean("enabled", isEnabled());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // 从父类的 CompoundTag 方法处理
    }
    
    // 26.1.2 新增的抽象方法
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 实体外壳不受伤害
        return false;
    }
    
    // 26.1.2 新增的抽象方法
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 无需同步数据
    }
}
