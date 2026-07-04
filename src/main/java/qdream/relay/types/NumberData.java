package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

/**
 * 数字类型
 * 执行时自动压入数据栈
 */
public class NumberData extends Data {
    private final double value;

    public NumberData(double value) {
        super("relay:number", 0,
                DataSignature.builder()
                        .output("relay:number")
                        .field("number", "Number")
                        .build());
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public double asDouble() {
        return value;
    }

    public int asInt() {
        return (int) value;
    }

    public double getValue() {
        return value;
    }

    public boolean isInteger() {
        return value == (int) value;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag value = new CompoundTag();
        value.putString("number", String.valueOf(this.value));
        tag.put("value", value);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        String value = tag.getCompound("value")
                .flatMap(ct -> ct.getString("number"))
                .orElse("0.0");
        return new NumberData(Double.parseDouble(value));
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject value = new JsonObject();
        value.addProperty("number", this.value);
        json.add("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        String value = json.get("value").getAsJsonObject()
                .get("number").getAsString();
        return new NumberData(Double.parseDouble(value));
    }

    @Override
    public String toString() {
        return "NumberData{value=" + value + "}";
    }
}
