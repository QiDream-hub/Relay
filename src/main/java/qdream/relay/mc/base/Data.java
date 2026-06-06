package qdream.relay.mc.base;

import com.google.gson.JsonObject;

public abstract class Data extends Operation {
    public Data(String id, int cost) {
        super(id, cost);
    }

    public abstract Data fromJson(JsonObject json);
    public abstract JsonObject toJson(Data data);
}
