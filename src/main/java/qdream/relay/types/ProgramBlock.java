package qdream.relay.types;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.NbtSerializer;
import qdream.relay.mc.base.Data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 程序块/列表类型
 * 存储一个 Executable 列表，执行时将列表展开到程序栈
 */
public class ProgramBlock extends Data {
    private final List<Executable> items;

    public ProgramBlock(List<Executable> items) {
        super("relay:list", 0);
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public ProgramBlock() {
        this(new ArrayList<>());
    }

    @Override
    public void execute(StateMachine executor) {
        // 列表执行时将自己压入数据栈
        executor.pushData(this);
    }

    /**
     * 将列表内容展开到程序栈
     * 用于 eval 操作
     */
    public void expandToProgramStack(StateMachine executor) {
        List<Executable> reversed = new ArrayList<>(items);
        java.util.Collections.reverse(reversed);
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

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void add(Executable item) {
        items.add(item);
    }

    public Executable get(int index) {
        return items.get(index);
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for ProgramBlock: " + id);
        }
        
        List<Executable> list = new ArrayList<>();
        JsonArray itemsArray = json.getAsJsonArray("value");
        if (itemsArray != null) {
            for (JsonElement itemElem : itemsArray) {
                if (itemElem.isJsonObject()) {
                    // 暂时返回空列表，TODO: 实现 JSON 反序列化
                    // Executable item = Executable.TypeRegistry.fromJson(itemElem.getAsJsonObject());
                }
            }
        }
        return new ProgramBlock(list);
    }

    @Override
    public JsonObject toJson(Data data) {
        ProgramBlock listData = (ProgramBlock) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        JsonArray itemsArray = new JsonArray();
        for (Executable item : listData.items) {
            // 暂时添加空对象，TODO: 实现 JSON 序列化
            // itemsArray.add(item.toJson());
        }
        json.add("value", itemsArray);
        return json;
    }

    /**
     * 从 NBT 列表创建 ProgramBlock
     */
    public static ProgramBlock fromNbtList(ListTag listTag) {
        List<Executable> list = NbtSerializer.INSTANCE.deserializeList(listTag);
        return new ProgramBlock(list);
    }

    /**
     * 序列化为 NBT 列表
     */
    public ListTag toNbtList() {
        return NbtSerializer.INSTANCE.serializeList(items);
    }
}
