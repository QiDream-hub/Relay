package qdream.relay.mc;

import com.google.gson.JsonElement;
import qdream.relay.engine.IData;
import qdream.relay.engine.IExecutable;
import qdream.relay.engine.IotaTypeRegistry;
import qdream.relay.engine.StateMachine;

import java.util.List;
import java.util.UUID;

/**
 * Minecraft Iota 实现
 * 可执行的数据单元，用于状态机
 */
public class McIota implements IExecutable {
    private final McIotaType type;
    private final Object value;

    private McIota(McIotaType type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String getType() {
        return type.toLowerCase();
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void execute(StateMachine executor) {
        // 非字符串、非列表类型不可执行
        if (type != McIotaType.STRING && type != McIotaType.LIST) {
            // 数据自动压入数据栈
            executor.pushData(this);
        } else if (type == McIotaType.STRING) {
            // 字符串类型作为操作执行
            executor.executeOperation((String) value);
        }
        // 列表类型暂不处理
    }

    @Override
    public JsonElement toJson() {
        return IotaTypeRegistry.toJson(this);
    }

    // ========== 工厂方法 ==========

    public static McIota ofInt(int value) {
        return new McIota(McIotaType.NUMBER, value);
    }

    public static McIota ofDouble(double value) {
        return new McIota(McIotaType.NUMBER, value);
    }

    public static McIota ofBoolean(boolean value) {
        return new McIota(McIotaType.BOOLEAN, value);
    }

    public static McIota ofVector(Object vec3) {
        return new McIota(McIotaType.VECTOR, vec3);
    }

    public static McIota ofString(String value) {
        return new McIota(McIotaType.STRING, value);
    }

    public static McIota ofEntity(UUID entityId) {
        return new McIota(McIotaType.ENTITY, entityId);
    }

    public static McIota ofList(List<IExecutable> value) {
        return new McIota(McIotaType.LIST, value);
    }

    public static McIota ofNull() {
        return new McIota(McIotaType.NULL, null);
    }

    // ========== 类型判断 ==========

    public boolean isNull() {
        return type == McIotaType.NULL;
    }

    public boolean isNumber() {
        return type == McIotaType.NUMBER;
    }

    public boolean isBoolean() {
        return type == McIotaType.BOOLEAN;
    }

    public boolean isVector() {
        return type == McIotaType.VECTOR;
    }

    public boolean isString() {
        return type == McIotaType.STRING;
    }

    public boolean isEntity() {
        return type == McIotaType.ENTITY;
    }

    public boolean isList() {
        return type == McIotaType.LIST;
    }

    // ========== 类型转换 ==========

    public double asDouble() {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        throw new UnsupportedOperationException("类型 " + type + " 无法转换为 double");
    }

    public int asInt() {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        throw new UnsupportedOperationException("类型 " + type + " 无法转换为 int");
    }

    public boolean asBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new UnsupportedOperationException("类型 " + type + " 无法转换为 boolean");
    }

    public Object asVector() {
        return value;
    }

    public String asString() {
        if (value instanceof String) {
            return (String) value;
        }
        throw new UnsupportedOperationException("类型 " + type + " 无法转换为字符串");
    }

    public UUID asEntity() {
        if (value instanceof UUID) {
            return (UUID) value;
        }
        throw new UnsupportedOperationException("类型 " + type + " 无法转换为实体");
    }

    @SuppressWarnings("unchecked")
    public List<IExecutable> asList() {
        if (value instanceof List) {
            return (List<IExecutable>) value;
        }
        throw new UnsupportedOperationException("类型 " + type + " 无法转换为列表");
    }
}
