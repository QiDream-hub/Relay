package qdream.relay.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Iota - 类型系统中的基本值类型
 * 所有在栈中存储的值都封装为 Iota
 */
public class Iota {
    private final IotaType type;
    private final Object value;

    private Iota(IotaType type, Object value) {
        this.type = type;
        this.value = value;
    }

    // ========== 静态工厂方法 ==========

    public static Iota ofNumber(Number value) {
        return new Iota(IotaType.NUMBER, value);
    }

    public static Iota ofInt(int value) {
        return new Iota(IotaType.NUMBER, value);
    }

    public static Iota ofDouble(double value) {
        return new Iota(IotaType.NUMBER, value);
    }

    public static Iota ofBoolean(boolean value) {
        return new Iota(IotaType.BOOLEAN, value);
    }

    public static Iota ofVector(Vec3 value) {
        return new Iota(IotaType.VECTOR, value);
    }

    public static Iota ofString(String value) {
        return new Iota(IotaType.STRING, value);
    }

    public static Iota ofEntity(UUID uuid) {
        return new Iota(IotaType.ENTITY, uuid);
    }

    public static Iota ofList(List<Iota> value) {
        return new Iota(IotaType.LIST, value);
    }

    public static Iota ofNull() {
        return new Iota(IotaType.NULL, null);
    }

    // ========== 类型判断 ==========

    public IotaType getType() {
        return type;
    }

    public boolean isNumber() {
        return type == IotaType.NUMBER;
    }

    public boolean isBoolean() {
        return type == IotaType.BOOLEAN;
    }

    public boolean isVector() {
        return type == IotaType.VECTOR;
    }

    public boolean isString() {
        return type == IotaType.STRING;
    }

    public boolean isEntity() {
        return type == IotaType.ENTITY;
    }

    public boolean isList() {
        return type == IotaType.LIST;
    }

    public boolean isNull() {
        return type == IotaType.NULL;
    }

    // ========== 值获取 ==========

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public Number asNumber() {
        if (type != IotaType.NUMBER) {
            throw new IllegalStateException("Cannot convert " + type + " to Number");
        }
        return (Number) value;
    }

    public int asInt() {
        if (type != IotaType.NUMBER) {
            throw new IllegalStateException("Cannot convert " + type + " to int");
        }
        Number n = (Number) value;
        return n instanceof Double ? ((Double) n).intValue() : n.intValue();
    }

    public double asDouble() {
        if (type != IotaType.NUMBER) {
            throw new IllegalStateException("Cannot convert " + type + " to double");
        }
        Number n = (Number) value;
        return n instanceof Double ? (Double) n : n.doubleValue();
    }

    public boolean asBoolean() {
        if (type != IotaType.BOOLEAN) {
            throw new IllegalStateException("Cannot convert " + type + " to boolean");
        }
        return (Boolean) value;
    }

    public Vec3 asVector() {
        if (type != IotaType.VECTOR) {
            throw new IllegalStateException("Cannot convert " + type + " to Vector");
        }
        return (Vec3) value;
    }

    public String asString() {
        if (type != IotaType.STRING) {
            throw new IllegalStateException("Cannot convert " + type + " to String");
        }
        return (String) value;
    }

    public UUID asEntity() {
        if (type != IotaType.ENTITY) {
            throw new IllegalStateException("Cannot convert " + type + " to Entity UUID");
        }
        return (UUID) value;
    }

    public List<Iota> asList() {
        if (type != IotaType.LIST) {
            throw new IllegalStateException("Cannot convert " + type + " to List");
        }
        return (List<Iota>) value;
    }

    // ========== NBT 序列化 ==========

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());

        switch (type) {
            case NUMBER -> {
                if (value instanceof Double) {
                    tag.putDouble("value", (Double) value);
                } else {
                    tag.putInt("value", ((Number) value).intValue());
                }
            }
            case BOOLEAN -> tag.putBoolean("value", (Boolean) value);
            case VECTOR -> {
                Vec3 v = (Vec3) value;
                tag.putDouble("x", v.x);
                tag.putDouble("y", v.y);
                tag.putDouble("z", v.z);
            }
            case STRING -> tag.putString("value", (String) value);
            case ENTITY -> tag.putString("value", ((UUID) value).toString());
            case LIST -> {
                ListTag list = new ListTag();
                for (Iota iota : (List<Iota>) value) {
                    list.add(iota.toNbt());
                }
                tag.put("value", list);
            }
            case NULL -> {
            }
        }

        return tag;
    }

    public static Iota fromNbt(CompoundTag tag) {
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
            case VECTOR -> Iota.ofVector(new Vec3(
                    tag.getDouble("x").orElse(0.0),
                    tag.getDouble("y").orElse(0.0),
                    tag.getDouble("z").orElse(0.0)));
            case STRING -> Iota.ofString(tag.getString("value").orElse(null));
            case ENTITY -> Iota.ofEntity(UUID.fromString(tag.getString("value").orElse("00000000-0000-0000-0000-000000000000")));
            case LIST -> {
                ListTag list = tag.getList("value").orElse(new ListTag());
                List<Iota> iotaList = new ArrayList<>();
                for (Tag element : list) {
                    iotaList.add(Iota.fromNbt((CompoundTag) element));
                }
                yield Iota.ofList(iotaList);
            }
            case NULL, ANY -> Iota.ofNull();
        };
    }

    // ========== 重写 equals 和 hashCode ==========

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Iota iota))
            return false;
        if (type != iota.type)
            return false;
        return value != null ? value.equals(iota.value) : iota.value == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return switch (type) {
            case NUMBER, BOOLEAN, STRING -> String.valueOf(value);
            case VECTOR -> ((Vec3) value).toString();
            case ENTITY -> "Entity[" + ((UUID) value).toString().substring(0, 8) + "]";
            case LIST -> "List[" + ((List<Iota>) value).size() + "]";
            case NULL, ANY -> "null";
        };
    }
}
