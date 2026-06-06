package qdream.relay.types;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 列表类型
 */
public class ListIota extends Data {
    private final List<Executable> value;

    public ListIota(List<Executable> value) {
        super("relay:list", 0);
        this.value = value != null ? new ArrayList<>(value) : new ArrayList<>();
    }

    public List<Executable> getValue() {
        return new ArrayList<>(value);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for ListIota: " + id);
        }
        // TODO: 实现 JSON 反序列化
        return new ListIota(new ArrayList<>());
    }

    @Override
    public JsonObject toJson(Data data) {
        ListIota listData = (ListIota) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        // TODO: 实现 JSON 序列化
        json.add("value", new JsonArray());
        return json;
    }
}
