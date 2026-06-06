package qdream.relay.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 程序块（列表）
 * 可执行单元，执行时会将列表内容反转后压入程序栈
 */
public class ProgramBlock implements Executable {
    private final List<Executable> items;

    public ProgramBlock(List<Executable> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    @Override
    public String getType() {
        return "relay:list";
    }

    @Override
    public Object getValue() {
        return items;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:list");
        JsonArray array = new JsonArray();
        for (Executable item : items) {
            array.add(item.toJson());
        }
        json.add("value", array);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        // 列表反转后压入程序栈，保证从左到右的执行顺序
        List<Executable> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);
        for (Executable item : reversed) {
            executor.pushProgram(item);
        }
    }

    public List<Executable> getItems() {
        return new ArrayList<>(items);
    }

    public int size() {
        return items.size();
    }
}
