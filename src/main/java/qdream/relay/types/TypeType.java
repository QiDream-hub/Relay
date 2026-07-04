package qdream.relay.types;

import com.google.gson.JsonObject;

import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

public class TypeType extends Data {

    private final String value;

    public TypeType(String id) {
        super("relay:type", 0, DataSignature.builder()
                .output("relay:type")
                .field("id", "String")
                .build());
        this.value = id;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public String getId() {
        return value;
    }

    /**
     * 比较两个 TypeType 是否相同
     * @param other 另一个 TypeType 实例
     * @return 如果两个类型的 id 相同则返回 true
     */
    public boolean equalsTo(TypeType other) {
        if (other == null) {
            return false;
        }
        if (this.value == null && other.value == null) {
            return true;
        }
        if (this.value == null || other.value == null) {
            return false;
        }
        return this.value.equals(other.value);
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag valueTag = new CompoundTag();
        valueTag.putString("id", this.value != null ? this.value : "");
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        String id = tag.getCompound("value").flatMap(ct -> ct.getString("id")).orElse("");
        return new TypeType(id);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject value = new JsonObject();
        value.addProperty("id", this.value);
        json.add("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            JsonObject value = json.get("value").getAsJsonObject();
            return new TypeType(value.get("id").getAsString());
        }
        return new TypeType("");
    }

}
