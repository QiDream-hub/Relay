package qdream.relay.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public static Iota ofVector(Vector3 value) {
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

    public Vector3 asVector() {
        if (type != IotaType.VECTOR) {
            throw new IllegalStateException("Cannot convert " + type + " to Vector");
        }
        return (Vector3) value;
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
            case VECTOR -> ((Vector3) value).toString();
            case ENTITY -> "Entity[" + ((UUID) value).toString().substring(0, 8) + "]";
            case LIST -> "List[" + ((List<Iota>) value).size() + "]";
            case NULL, ANY -> "null";
        };
    }
}
