package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

/**
 * 空值类型
 * 执行时自动压入数据栈
 */
public class NullIType extends Data {
    public static final NullIType INSTANCE = new NullIType();

    private NullIType() {
        super("relay:null", 0,
                DataSignature.builder()
                        .output("relay:null")
                        .build());
    }

    /**
     * 创建 NullIota 实例
     * 注意：优先使用 INSTANCE 单例
     */
    public NullIType(int ignored) {
        // 兼容工厂方法调用，实际使用单例
        this();
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        return INSTANCE;
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
    }

    @Override
    public Data fromJson(JsonObject json) {
        return INSTANCE;
    }
}
