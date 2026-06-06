package qdream.relay.mc.base;

import net.minecraft.nbt.CompoundTag;

public abstract class Data extends Operation {
    public Data(String id, int cost) {
        super(id, cost);
    }

    /**
     * 序列化为 NBT 标签
     * @param tag 要写入的 CompoundTag（已经包含 id 字段）
     */
    public abstract void toNbt(CompoundTag tag);

    /**
     * 从 NBT 标签反序列化
     * @param tag NBT 标签（已包含 id 字段）
     * @return 反序列化后的 Data 实例
     */
    public abstract Data fromNbt(CompoundTag tag);
}
