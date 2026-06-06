package qdream.relay.types;

import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 布尔类型
 * 执行时自动压入数据栈
 */
public class BooleanIota extends Data {
    private final boolean value;

    public BooleanIota(boolean value) {
        super("relay:boolean", 0);
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public boolean asBoolean() {
        return value;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        tag.putBoolean("value", value);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        return new BooleanIota(tag.getBoolean("value").orElse(false));
    }
}
