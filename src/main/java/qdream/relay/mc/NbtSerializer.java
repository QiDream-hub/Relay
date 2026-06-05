package qdream.relay.mc;

import qdream.relay.engine.IExecutable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Minecraft NBT 序列化工具类
 */
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    private NbtSerializer() {}

    /**
     * 静态序列化方法
     */
    public static CompoundTag serializeStatic(McIota iota) {
        return INSTANCE.serialize(iota);
    }

    /**
     * 静态反序列化方法
     */
    public static McIota deserializeStatic(CompoundTag tag) {
        return INSTANCE.deserialize(tag);
    }

    public CompoundTag serialize(McIota iota) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", iota.getType());

        switch (iota.getType()) {
            case "number" -> {
                if (iota.getValue() instanceof Double) {
                    tag.putDouble("value", iota.asDouble());
                } else {
                    tag.putInt("value", iota.asInt());
                }
            }
            case "boolean" -> tag.putBoolean("value", iota.asBoolean());
            case "vector" -> {
                Object vec = iota.asVector();
                if (vec instanceof McVec3Adapter) {
                    Vec3 v = ((McVec3Adapter) vec).getVec3();
                    tag.putDouble("x", v.x);
                    tag.putDouble("y", v.y);
                    tag.putDouble("z", v.z);
                }
            }
            case "string" -> tag.putString("value", iota.asString());
            case "entity" -> tag.putString("value", iota.asEntity().toString());
            case "list" -> tag.put("value", serializeList(iota.asList()));
            case "null", "any" -> {}
        }

        return tag;
    }

    public McIota deserialize(CompoundTag tag) {
        String typeName = tag.getString("type").orElse("unknown");

        return switch (typeName) {
            case "number" -> {
                if (tag.contains("value")) {
                    yield McIota.ofDouble(tag.getDouble("value").orElse(0.0));
                } else {
                    yield McIota.ofInt(tag.getInt("value").orElse(0));
                }
            }
            case "boolean" -> McIota.ofBoolean(tag.getBoolean("value").orElse(false));
            case "vector" -> McIota.ofVector(new McVec3Adapter(new Vec3(
                    tag.getDouble("x").orElse(0.0),
                    tag.getDouble("y").orElse(0.0),
                    tag.getDouble("z").orElse(0.0))));
            case "string" -> McIota.ofString(tag.getString("value").orElse(null));
            case "entity" -> McIota.ofEntity(UUID.fromString(tag.getString("value").orElse("00000000-0000-0000-0000-000000000000")));
            case "list" -> McIota.ofList(deserializeList(tag.getList("value").orElse(new ListTag())));
            case "null", "any" -> McIota.ofNull();
            default -> McIota.ofNull();
        };
    }

    public ListTag serializeList(List<IExecutable> list) {
        ListTag listTag = new ListTag();
        for (IExecutable iota : list) {
            if (iota instanceof McIota) {
                listTag.add(serialize((McIota) iota));
            }
        }
        return listTag;
    }

    public List<IExecutable> deserializeList(ListTag listTag) {
        List<IExecutable> list = new ArrayList<>();
        for (Tag element : listTag) {
            list.add(deserialize((CompoundTag) element));
        }
        return list;
    }
}
