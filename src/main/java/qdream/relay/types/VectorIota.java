package qdream.relay.types;

import com.google.gson.JsonObject;

import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 向量类型
 * 执行时自动压入数据栈
 */
public class VectorIota extends Data {
    private final Vec3 value;

    public VectorIota(Vec3 value) {
        super("relay:vector", 0);
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public Vec3 asVector() {
        return value;
    }

    public Vec3 getVec3() {
        return value;
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for VectorIota: " + id);
        }
        JsonObject posJson = json.getAsJsonObject("value");
        double x = posJson.get("x").getAsDouble();
        double y = posJson.get("y").getAsDouble();
        double z = posJson.get("z").getAsDouble();
        return new VectorIota(new Vec3(x, y, z));
    }

    @Override
    public JsonObject toJson(Data data) {
        VectorIota vectorData = (VectorIota) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        JsonObject posJson = new JsonObject();
        posJson.addProperty("x", vectorData.value.x);
        posJson.addProperty("y", vectorData.value.y);
        posJson.addProperty("z", vectorData.value.z);
        json.add("value", posJson);
        return json;
    }
}
