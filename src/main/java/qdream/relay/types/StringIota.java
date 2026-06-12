package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 字符串类型
 * 执行时自动压入数据栈
 */
public class StringIota extends Data {
    private final String value;

    public StringIota(String value) {
        super("relay:string", 0);
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
        tag.putString("value", value);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        return new StringIota(tag.getString("value").orElse(""));
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        json.addProperty("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        return new StringIota(json.has("value") ? json.get("value").getAsString() : "");
    }
}
