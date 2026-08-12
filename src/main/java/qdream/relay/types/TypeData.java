package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

public class TypeData extends Data {

    private final String value;

    public TypeData(String id) {
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

    /**
     * 获取存储的类型 ID
     * @return 存储的类型 ID（如 "relay:number"）
     */
    public String getValue() {
        return value;
    }

    /**
     * 比较两个 TypeData 是否相同
     * 
     * @param other 另一个 TypeData 实例
     * @return 如果两个类型的 id 相同则返回 true
     */
    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof TypeData)) {
            return false;
        }
        TypeData that = (TypeData) other;
        if (this.value == null && that.value == null) {
            return true;
        }
        if (this.value == null || that.value == null) {
            return false;
        }
        return this.value.equals(that.value);
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
        return new TypeData(id);
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
            return new TypeData(value.get("id").getAsString());
        }
        return new TypeData("");
    }

    @Override
    public Component asString() {
        return Component.literal(value != null ? value : "");
    }

    @Override
    public boolean asBoolean() {
        return value != null && !value.isEmpty();
    }
}
