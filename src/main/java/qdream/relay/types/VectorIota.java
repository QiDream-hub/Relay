package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 向量类型
 * 执行时自动压入数据栈
 */
public class VectorIota extends Data {
    private final Vec3 vec3;

    public VectorIota(Vec3 vec3) {
        super("relay:vector", 0);
        this.vec3 = vec3;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public Vec3 asVector() {
        return vec3;
    }

    public Vec3 getVec3() {
        return vec3;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag vecTag = new CompoundTag();
        vecTag.putDouble("x", vec3.x);
        vecTag.putDouble("y", vec3.y);
        vecTag.putDouble("z", vec3.z);
        tag.put("value", vecTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag vecTag = tag.getCompound("value").orElse(new CompoundTag());
        return new VectorIota(new Vec3(
            vecTag.getDouble("x").orElse(0.0),
            vecTag.getDouble("y").orElse(0.0),
            vecTag.getDouble("z").orElse(0.0)
        ));
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject vecJson = new JsonObject();
        vecJson.addProperty("x", vec3.x);
        vecJson.addProperty("y", vec3.y);
        vecJson.addProperty("z", vec3.z);
        json.add("value", vecJson);
    }

    @Override
    public Data fromJson(JsonObject json) {
        JsonObject vecJson = json.getAsJsonObject("value");
        return new VectorIota(new Vec3(
            vecJson.has("x") ? vecJson.get("x").getAsDouble() : 0.0,
            vecJson.has("y") ? vecJson.get("y").getAsDouble() : 0.0,
            vecJson.has("z") ? vecJson.get("z").getAsDouble() : 0.0
        ));
    }
}
