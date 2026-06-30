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
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

/**
 * 列表类型
 */
public class ListType extends Data {
    private final List<Executable> value;

    public ListType(List<Executable> value) {
        super("relay:list", 0,
                DataSignature.builder()
                        .output("relay:list")
                        .field("list", "relay:list")
                        .build());
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
        CompoundTag valueTag = new CompoundTag();
        valueTag.put("list", listTag);
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        List<Executable> list = tag.getCompound("value")
                .flatMap(ct -> ct.getList("list"))
                .map(listTag -> {
                    List<Executable> result = new ArrayList<>();
                    for (Tag element : listTag) {
                        if (element instanceof CompoundTag compoundTag) {
                            String id = compoundTag.getString("id").orElse("");
                            OperationRegistry.getEntry(id).ifPresent(entry -> {
                                Operation instance = (Operation) entry.create();
                                result.add(instance.fromNbt(compoundTag));
                            });
                        }
                    }
                    return result;
                })
                .orElse(new ArrayList<>());

        return new ListType(list);
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
        JsonObject valueObject = new JsonObject();
        valueObject.add("list", array);
        json.add("value", valueObject);
    }

    @Override
    public Data fromJson(JsonObject json) {
        List<Executable> list = new ArrayList<>();

        if (json.has("value") && json.get("value").isJsonObject()) {
            JsonObject valueObject = json.getAsJsonObject("value");

            if (valueObject.has("list") && valueObject.get("list").isJsonArray()) {
                JsonArray array = valueObject.getAsJsonArray("list");

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
            }
        }

        return new ListType(list);
    }
}