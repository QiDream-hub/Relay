package qdream.relay.types;

import java.util.List;
import java.util.StringJoiner;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.errors.CompilationException;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.tools.TextTools;

/**
 * 列表类型
 */
public class ListData extends Data {
    private final List<Executable> value;

    public ListData(List<Executable> value) {
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
        ListTag listTag = ProgramCompiler.toNbt(value);
        CompoundTag valueTag = new CompoundTag();
        valueTag.put("list", listTag);
        tag.put("value", valueTag);

    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        List<Executable> list = tag.getCompound("value")
                .flatMap(ct -> ct.getList("list"))
                .map(ProgramCompiler::fromNbt)
                .orElse(new ArrayList<>());
        return new ListData(list);

    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonArray array = ProgramCompiler.toJson(value);
        JsonObject valueObject = new JsonObject();
        valueObject.add("list", array);
        json.add("value", valueObject);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value") && json.get("value").isJsonObject()) {
            JsonObject valueObject = json.getAsJsonObject("value");
            if (valueObject.has("list") && valueObject.get("list").isJsonArray()) {
                List<Executable> list = ProgramCompiler.fromJson(valueObject.getAsJsonArray("list"));
                return new ListData(list);

            }
        }
        return new ListData(new ArrayList<>());
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof ListData that)) {
            return false;
        }
        if (this.value.size() != that.value.size()) {
            return false;
        }
        for (int i = 0; i < this.value.size(); i++) {
            Executable thisElem = this.value.get(i);
            Executable thatElem = that.value.get(i);
            if (thisElem instanceof Operation && thatElem instanceof Operation) {
                if (!((Operation) thisElem).equalsTo((Operation) thatElem)) {
                    return false;
                }
            } else if (!thisElem.equals(thatElem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Component asString() {
        return TextTools.formatList(this.value);
    }

    @Override
    public boolean asBoolean() {
        return !value.isEmpty();
    }
}