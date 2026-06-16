package qdream.relay.mc.base;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

import qdream.relay.engine.Executable;
import qdream.relay.mc.signature.Signature;

/**
 * 操作基类
 * 所有操作和数据类型的公共父类，提供默认的序列化/反序列化实现（仅处理 id）。
 * 子类（如 Data）可 override 以添加额外字段。
 */
public abstract class Operation implements Executable {
    protected final String id;
    protected final int cost;
        


    public Operation(String id, int cost) {
        this.id = id;
        this.cost = cost;
    }

    public String getId() {
        return id;
    }

    @Override
    public int getCost() {
        return cost;
    }

    public abstract Signature getSignature();

    // ========== JSON 序列化/反序列化 ==========

    /**
     * 序列化为 JSON（默认写入 id 字段）
     */
    public void toJson(JsonObject json) {
        json.addProperty("id", id);
    }

    /**
     * 从 JSON 反序列化（默认返回自身，适用于无状态单例操作）
     */
    public Operation fromJson(JsonObject json) {
        return this;
    }

    // ========== NBT 序列化/反序列化 ==========

    /**
     * 序列化为 NBT（默认写入 id 字段）
     */
    public void toNbt(CompoundTag tag) {
        tag.putString("id", id);
    }

    /**
     * 从 NBT 反序列化（默认返回自身，适用于无状态单例操作）
     */
    public Operation fromNbt(CompoundTag tag) {
        return this;
    }
}
