package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.McVec3Adapter;

/**
 * 向量类型
 * 执行时自动压入数据栈
 */
public class VectorIota implements Executable {
    private final McVec3Adapter value;

    public VectorIota(McVec3Adapter value) {
        this.value = value;
    }

    public VectorIota(net.minecraft.world.phys.Vec3 value) {
        this.value = new McVec3Adapter(value);
    }

    @Override
    public String getId() {
        return "relay:vector";
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", "relay:vector");
        JsonObject posJson = new JsonObject();
        posJson.addProperty("x", value.x());
        posJson.addProperty("y", value.y());
        posJson.addProperty("z", value.z());
        json.add("value", posJson);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public McVec3Adapter asVector() {
        return value;
    }

    public net.minecraft.world.phys.Vec3 getVec3() {
        return value.getVec3();
    }
}
