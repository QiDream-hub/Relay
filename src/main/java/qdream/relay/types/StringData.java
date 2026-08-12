package qdream.relay.types;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

/**
 * 字符串类型
 * 执行时自动压入数据栈
 */
public class StringData extends Data {
    private final Component value;

    public StringData(Component value) {
        super("relay:string", 0,
                DataSignature.builder()
                        .output("relay:string")
                        .field("string", "String")
                        .build());
        this.value = value;
    }

    public Component getValue() {
        return value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, this.value)
            .result().ifPresent(valueTag -> tag.put("value", valueTag));
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        Tag valueTag = tag.get("value");
        if (valueTag == null) {
            return new StringData(Component.literal(""));
        }
        Component value = ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, valueTag)
            .result()
            .map(pair -> pair.getFirst())
            .orElse(Component.literal(""));
        return new StringData(value);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, this.value)
            .result().ifPresent(jsonElement -> json.add("string", jsonElement));
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("string")) {
            Component value = ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, json.get("string"))
                .result()
                .map(pair -> pair.getFirst())
                .orElse(Component.literal(""));
            return new StringData(value);
        }
        return new StringData(Component.literal(""));
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof StringData)) {
            return false;
        }
        StringData that = (StringData) other;
        return this.value != null ? this.value.equals(that.value) : that.value == null;
    }

    @Override
    public boolean asBoolean() {
        return value != null && !value.getString().isEmpty();
    }

    @Override
    public Component asString() {
        return value;
    }
}
