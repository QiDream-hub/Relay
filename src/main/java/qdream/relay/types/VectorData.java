package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

/**
 * 向量类型
 * 执行时自动压入数据栈
 */
public class VectorData extends Data {
    private final Vec3 vec3;

    public VectorData(Vec3 vec3) {
        super("relay:vector", 0,
                DataSignature.builder()
                        .output("relay:vector")
                        .field("x", "Number")
                        .field("y", "Number")
                        .field("z", "Number")
                        .build());
        this.vec3 = vec3 != null ? vec3 : new Vec3(0, 0, 0);
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
        Vec3 vec = tag.getCompound("value")
                .map(ct -> new Vec3(
                        ct.getDouble("x").orElse(0.0),
                        ct.getDouble("y").orElse(0.0),
                        ct.getDouble("z").orElse(0.0)))
                .orElse(new Vec3(0, 0, 0));

        return new VectorData(vec);
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
        Vec3 vec = new Vec3(0, 0, 0);

        if (json.has("value") && json.get("value").isJsonObject()) {
            JsonObject vecJson = json.getAsJsonObject("value");
            vec = new Vec3(
                    vecJson.has("x") ? vecJson.get("x").getAsDouble() : 0.0,
                    vecJson.has("y") ? vecJson.get("y").getAsDouble() : 0.0,
                    vecJson.has("z") ? vecJson.get("z").getAsDouble() : 0.0);
        }

        return new VectorData(vec);
    }

    @Override
    public String toString() {
        return "VectorData{x=" + vec3.x + ", y=" + vec3.y + ", z=" + vec3.z + "}";
    }
}