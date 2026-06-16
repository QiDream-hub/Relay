package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.SignatureName;

/**
 * 数字类型
 * 执行时自动压入数据栈
 */
public class NumberIota extends Data {
    private final double value;

    public NumberIota(double value) {
        super("relay:number", 0,
                DataSignature.builder()
                        .output("relay:number")
                        .input(SignatureName.builder().setName("number").setType("Number").build())
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
        if (isInteger()) {
            tag.putInt("value", asInt());
        } else {
            tag.putDouble("value", asDouble());
        }
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        var intOpt = tag.getInt("value");
        if (intOpt.isPresent()) {
            return new NumberIota(intOpt.get());
        } else {
            return new NumberIota(tag.getDouble("value").orElse(0.0));
        }
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        json.addProperty("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            return new NumberIota(json.get("value").getAsDouble());
        }
        return new NumberIota(0);
    }
}
