package qdream.relay.types;

import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 空值类型
 * 执行时自动压入数据栈
 */
public class NullIota extends Data {
    public static final NullIota INSTANCE = new NullIota();

    private NullIota() {
        super("relay:null", 0);
    }

    /**
     * 创建 NullIota 实例
     * 注意：优先使用 INSTANCE 单例
     */
    public NullIota(int ignored) {
        // 兼容工厂方法调用，实际使用单例
        this();
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public void toNbt(CompoundTag tag) {
        // NullIota 没有数据需要序列化
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        return INSTANCE;
    }
}
