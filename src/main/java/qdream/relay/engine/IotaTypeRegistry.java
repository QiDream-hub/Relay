package qdream.relay.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Iota 类型注册表
 * 管理所有数据类型的注册和 JSON 序列化/反序列化
 */
public class IotaTypeRegistry {

    /**
     * 序列化器/反序列化器对
     */
    public static class SerDePair {
        final Executable.JsonElementSerializer serializer;
        final Executable.JsonElementDeserializer deserializer;

        public SerDePair(Executable.JsonElementSerializer serializer, Executable.JsonElementDeserializer deserializer) {
            this.serializer = serializer;
            this.deserializer = deserializer;
        }
    }

    private static final Map<String, SerDePair> TYPES = new HashMap<>();

    /**
     * 注册类型
     * @param typeId 类型 ID
     * @param serializer 序列化器
     * @param deserializer 反序列化器
     */
    public static void register(String typeId, Executable.JsonElementSerializer serializer, Executable.JsonElementDeserializer deserializer) {
        TYPES.put(typeId, new SerDePair(serializer, deserializer));
    }

    /**
     * 从 JSON 反序列化
     * @param json JSON 元素，必须包含 "id" 字段
     * @return 反序列化后的 Executable
     */
    public static Executable fromJson(JsonElement json) {
        if (!json.isJsonObject()) {
            throw new RuntimeException("JSON 必须是对象");
        }
        JsonObject obj = json.getAsJsonObject();
        String id = obj.get("id").getAsString();
        SerDePair serDe = TYPES.get(id);
        if (serDe == null) {
            throw new RuntimeException("未知类型：" + id);
        }
        return serDe.deserializer.deserialize(obj);
    }

    /**
     * 序列化 Executable 为 JSON
     * @param data 数据实例
     * @return JSON 元素
     */
    public static JsonElement toJson(Executable data) {
        String typeId = data.getId();
        SerDePair serDe = TYPES.get(typeId);
        if (serDe == null) {
            throw new RuntimeException("未注册类型：" + typeId);
        }
        return serDe.serializer.serialize(data);
    }

    /**
     * 私有构造函数，防止实例化
     */
    private IotaTypeRegistry() {}
}
