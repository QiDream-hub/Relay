package qdream.relay.mc;

import qdream.relay.engine.Iota;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.Vector3;

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

    public CompoundTag serialize(Iota iota) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", iota.getType().name());

        switch (iota.getType()) {
            case NUMBER -> {
                if (iota.getValue() instanceof Double) {
                    tag.putDouble("value", iota.asDouble());
                } else {
                    tag.putInt("value", iota.asInt());
                }
            }
            case BOOLEAN -> tag.putBoolean("value", iota.asBoolean());
            case VECTOR -> {
                Vector3 v = iota.asVector();
                tag.putDouble("x", v.x());
                tag.putDouble("y", v.y());
                tag.putDouble("z", v.z());
            }
            case STRING -> tag.putString("value", iota.asString());
            case ENTITY -> tag.putString("value", iota.asEntity().toString());
            case LIST -> tag.put("value", serializeList(iota.asList()));
            case NULL -> {}
            case ANY -> {}
        }

        return tag;
    }

    public Iota deserialize(CompoundTag tag) {
        String typeName = tag.getString("type").orElse("unknown");
        IotaType type = IotaType.valueOf(typeName);

        return switch (type) {
            case NUMBER -> {
                if (tag.contains("value")) {
                    yield Iota.ofDouble(tag.getDouble("value").orElse(0.0));
                } else {
                    yield Iota.ofInt(tag.getInt("value").orElse(0));
                }
            }
            case BOOLEAN -> Iota.ofBoolean(tag.getBoolean("value").orElse(false));
            case VECTOR -> Iota.ofVector(new McVec3Adapter(new Vec3(
                    tag.getDouble("x").orElse(0.0),
                    tag.getDouble("y").orElse(0.0),
                    tag.getDouble("z").orElse(0.0))));
            case STRING -> Iota.ofString(tag.getString("value").orElse(null));
            case ENTITY -> Iota.ofEntity(UUID.fromString(tag.getString("value").orElse("00000000-0000-0000-0000-000000000000")));
            case LIST -> Iota.ofList(deserializeList(tag.getList("value").orElse(new ListTag())));
            case NULL, ANY -> Iota.ofNull();
        };
    }

    public ListTag serializeList(List<Iota> list) {
        ListTag listTag = new ListTag();
        for (Iota iota : list) {
            listTag.add(serialize(iota));
        }
        return listTag;
    }

    public List<Iota> deserializeList(ListTag listTag) {
        List<Iota> list = new ArrayList<>();
        for (Tag element : listTag) {
            list.add(deserialize((CompoundTag) element));
        }
        return list;
    }
}
