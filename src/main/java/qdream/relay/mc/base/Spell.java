package qdream.relay.mc.base;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

import qdream.relay.mc.OperationSignature;

public abstract class Spell extends Operation {
    protected final OperationSignature signature;

    public Spell(String id, int cost, OperationSignature signature) {
        super(id, cost);
        this.signature = signature;
    }

    public OperationSignature getSignature() {
        return signature;
    }

    /**
     * 序列化为 NBT 标签
     * @param tag 要写入的 CompoundTag（已经包含 id 字段）
     */
    public void toNbt(CompoundTag tag) {
        tag.putString("id", id);
    }

    /**
     * 从 NBT 标签反序列化
     * @param tag NBT 标签（已包含 id 字段）
     * @return 反序列化后的 Spell 实例（对于单例操作，返回 this）
     */
    public Spell fromNbt(CompoundTag tag) {
        // 操作是单例，不需要反序列化，返回自身
        return this;
    }

    /**
     * 序列化为 JSON 对象
     * @param json 要写入的 JsonObject（已经包含 id 字段）
     */
    public void toJson(JsonObject json) {
        json.addProperty("id", id);
    }

    /**
     * 从 JSON 对象反序列化
     * @param json JSON 对象（已包含 id 字段）
     * @return 反序列化后的 Spell 实例（对于单例操作，返回 this）
     */
    public Spell fromJson(JsonObject json) {
        // 操作是单例，不需要反序列化，返回自身
        return this;
    }
}