package qdream.relay.mc.base;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

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

    public int getCost() {
        return cost;
    }

    public JsonObject toJson() {
        // 使用一个简单的 JSON 对象来表示这个操作
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        return json;
    }
}
