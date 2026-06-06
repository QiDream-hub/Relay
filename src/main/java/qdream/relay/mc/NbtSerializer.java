package qdream.relay.mc;

import qdream.relay.engine.Executable;
import qdream.relay.types.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Minecraft NBT 序列化工具类
 * 支持所有 Executable 类型的序列化/反序列化
 */
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    private NbtSerializer() {}

    /**
     * 静态序列化方法
     */
    public static CompoundTag serializeStatic(Executable exec) {
        return INSTANCE.serialize(exec);
    }

    /**
     * 静态反序列化方法
     */
    public static Executable deserializeStatic(CompoundTag tag) {
        return INSTANCE.deserialize(tag);
    }

    public CompoundTag serialize(Executable exec) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", exec.getType());

        if (exec instanceof NumberIota num) {
            if (num.isInteger()) {
                tag.putInt("value", num.asInt());
            } else {
                tag.putDouble("value", num.asDouble());
            }
        } else if (exec instanceof BooleanIota bool) {
            tag.putBoolean("value", bool.asBoolean());
        } else if (exec instanceof StringIota str) {
            tag.putString("value", str.asString());
        } else if (exec instanceof VectorIota vec) {
            Vec3 v = vec.getVec3();
            tag.putDouble("x", v.x);
            tag.putDouble("y", v.y);
            tag.putDouble("z", v.z);
        } else if (exec instanceof EntityIota ent) {
            tag.putString("value", ent.asEntity().toString());
        } else if (exec instanceof ProgramBlock list) {
            tag.put("value", serializeList(list.getItems()));
        } else if (exec instanceof Operation op) {
            tag.putString("op", op.getOpId());
        } else if (exec instanceof NullIota) {
            // 无值
        }

        return tag;
    }

    public Executable deserialize(CompoundTag tag) {
        String typeName = tag.getString("type").orElse("unknown");

        return switch (typeName) {
            case "relay:number" -> {
                if (tag.contains("value")) {
                    var intOpt = tag.getInt("value");
                    if (intOpt.isPresent()) {
                        yield new NumberIota(intOpt.get());
                    } else {
                        yield new NumberIota(tag.getDouble("value").orElse(0.0));
                    }
                } else {
                    yield new NumberIota(0);
                }
            }
            case "relay:boolean" -> new BooleanIota(tag.getBoolean("value").orElse(false));
            case "relay:vector" -> new VectorIota(new Vec3(
                    tag.getDouble("x").orElse(0.0),
                    tag.getDouble("y").orElse(0.0),
                    tag.getDouble("z").orElse(0.0)));
            case "relay:string" -> new StringIota(tag.getString("value").orElse(""));
            case "relay:entity" -> {
                String uuidStr = tag.getString("value").orElse("");
                yield new EntityIota(uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr));
            }
            case "relay:list" -> {
                ListTag listTag = tag.getList("value").orElse(new ListTag());
                yield new ProgramBlock(deserializeList(listTag));
            }
            case "relay:operation" -> new Operation(tag.getString("op").orElse(""));
            case "relay:null", "null", "any" -> NullIota.INSTANCE;
            default -> NullIota.INSTANCE;
        };
    }

    public ListTag serializeList(List<Executable> list) {
        ListTag listTag = new ListTag();
        for (Executable iota : list) {
            listTag.add(serialize(iota));
        }
        return listTag;
    }

    public List<Executable> deserializeList(ListTag listTag) {
        List<Executable> list = new ArrayList<>();
        for (Tag element : listTag) {
            list.add(deserialize((CompoundTag) element));
        }
        return list;
    }
}
