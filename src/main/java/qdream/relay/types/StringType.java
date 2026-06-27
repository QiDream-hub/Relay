package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.SignatureName;

/**
 * 字符串类型
 * 执行时自动压入数据栈
 */
public class StringType extends Data {
    private final String value;

    public StringType(String value) {
        super("relay:string", 0,
                DataSignature.builder()
                        .output("relay:string")
                        .input(SignatureName.builder().setName("string").setType("String").build())
                        .build());
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public String asString() {
        return value;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag value = new CompoundTag();
        value.putString("string", this.value != null ? this.value : "");
        tag.put("value", value);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        String string = tag.getCompound("value").flatMap(ct -> ct.getString("string")).orElse("");
        return new StringType(string);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject value = new JsonObject();
        value.addProperty("string", this.value);
        json.add("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            JsonObject value = json.get("value").getAsJsonObject();
            return new StringType(value.get("string").getAsString());
        }
        return new StringType("");
    }
}
