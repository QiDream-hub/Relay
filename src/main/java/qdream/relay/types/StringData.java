package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

/**
 * 字符串类型
 * 执行时自动压入数据栈
 */
public class StringData extends Data {
    private final String value;

    public StringData(String value) {
        super("relay:string", 0,
                DataSignature.builder()
                        .output("relay:string")
                        .field("string", "String")
                        .build());
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
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
        return new StringData(string);
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
            return new StringData(value.get("string").getAsString());
        }
        return new StringData("");
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof StringData)) {
            return false;
        }
        StringData that = (StringData) other;
        return this.value != null ? this.value.equals(that.value) : that.value == null;
    }

}
