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
        tag.putDouble("x", vec3.x);
        tag.putDouble("y", vec3.y);
        tag.putDouble("z", vec3.z);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        return new VectorIota(new Vec3(
            tag.getDouble("x").orElse(0.0),
            tag.getDouble("y").orElse(0.0),
            tag.getDouble("z").orElse(0.0)
        ));
    }

    @Override
    public void toJson(JsonObject json) {
        json.addProperty("x", vec3.x);
        json.addProperty("y", vec3.y);
        json.addProperty("z", vec3.z);
    }

    @Override
    public Data fromJson(JsonObject json) {
        return new VectorIota(new Vec3(
            json.has("x") ? json.get("x").getAsDouble() : 0.0,
            json.has("y") ? json.get("y").getAsDouble() : 0.0,
            json.has("z") ? json.get("z").getAsDouble() : 0.0
        ));
    }
}
