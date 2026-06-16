package qdream.relay.mc.base;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.mc.signature.DataSignature;

/**
 * 数据类型基类
 * 有状态的 Executable，执行时将自身压入数据栈。
 * 序列化方法继承自 Operation（写入 id），子类 override 以添加自身字段。
 */
public abstract class Data extends Operation {
    protected final DataSignature signature;

    public Data(String id, int cost, DataSignature signature) {
        super(id, cost);
        this.signature = signature;
    }

    /**
     * 从 NBT 反序列化，返回新的 Data 实例
     */
    @Override
    public abstract Data fromNbt(CompoundTag tag);

    /**
     * 从 JSON 反序列化，返回新的 Data 实例
     */
    @Override
    public abstract Data fromJson(JsonObject json);

    @Override
    public DataSignature getSignature() {
        return signature;
    }
}
