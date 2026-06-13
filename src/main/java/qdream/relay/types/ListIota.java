package qdream.relay.types;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;

/**
 * 列表类型
 */
public class ListIota extends Data {
    private final List<Executable> value;

    public ListIota(List<Executable> value) {
        super("relay:list", 0,
                OperationSignature.builder()
                        .output("relay:list")
                        .input("relay:list")
                        .build()
        );
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
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        ListTag listTag = new ListTag();
        for (Executable item : value) {
            CompoundTag itemTag = new CompoundTag();
            ((Operation) item).toNbt(itemTag);
            listTag.add(itemTag);
        }
        tag.put("value", listTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        var valueOpt = tag.getList("value");
        if (valueOpt.isPresent()) {
            ListTag listTag = valueOpt.get();
            List<Executable> list = new ArrayList<>();
            for (Tag element : listTag) {
                if (element instanceof CompoundTag compoundTag) {
                    String id = compoundTag.getString("id").orElse("");
                    OperationRegistry.getEntry(id).ifPresent(entry -> {
                        Operation instance = (Operation) entry.create();
                        list.add(instance.fromNbt(compoundTag));
                    });
                }
            }
            return new ListIota(list);
        } else {
            return new ListIota(new ArrayList<>());
        }
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonArray array = new JsonArray();
        for (Executable item : value) {
            JsonObject itemJson = new JsonObject();
            ((Operation) item).toJson(itemJson);
            array.add(itemJson);
        }
        json.add("value", array);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value") && json.get("value").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("value");
            List<Executable> list = new ArrayList<>();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    String id = obj.has("id") ? obj.get("id").getAsString() : "";
                    OperationRegistry.getEntry(id).ifPresent(entry -> {
                        Operation instance = (Operation) entry.create();
                        list.add(instance.fromJson(obj));
                    });
                }
            }
            return new ListIota(list);
        }
        return new ListIota(new ArrayList<>());
    }
}
