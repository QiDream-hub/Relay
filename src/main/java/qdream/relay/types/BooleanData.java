package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

/**
 * 布尔类型
 * 执行时自动压入数据栈
 */
public class BooleanData extends Data {
    private final boolean value;

    public BooleanData(boolean value) {
        super("relay:boolean", 0,
                DataSignature.builder()
                        .output("relay:boolean")
                        .field("boolean", "Boolean")
                        .build());
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public boolean asBoolean() {
        return value;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag value = new CompoundTag();
        value.putBoolean("boolean", this.value);
        tag.put("value", value);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        // 方式1：使用 Optional 链式调用
        Boolean value = tag.getCompound("value")
                .flatMap(ct -> ct.getBoolean("boolean"))
                .orElse(false);

        return new BooleanData(value);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject value = new JsonObject();
        value.addProperty("boolean", this.value);
        json.add("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            JsonObject jsonObject = json.get("value").getAsJsonObject();
            return new BooleanData(jsonObject.has("boolean") && jsonObject.get("boolean").getAsBoolean());
        }
        return new BooleanData(false);
    }

    @Override
    public String toString() {
        return "BooleanData{value=" + value + "}";
    }
}
