package qdream.relay.mc.base;

import com.google.gson.JsonObject;

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

    public JsonObject toJson() {
        // 使用一个简单的 JSON 对象来表示这个操作
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        return json;
    }

}