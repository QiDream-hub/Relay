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
        final IData.JsonElementSerializer serializer;
        final IData.JsonElementDeserializer deserializer;

        public SerDePair(IData.JsonElementSerializer serializer, IData.JsonElementDeserializer deserializer) {
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
    public static void register(String typeId, IData.JsonElementSerializer serializer, IData.JsonElementDeserializer deserializer) {
        TYPES.put(typeId, new SerDePair(serializer, deserializer));
    }

    /**
     * 从 JSON 反序列化
     * @param json JSON 元素，必须包含 "type" 字段
     * @return 反序列化后的 IData
     */
    public static IData fromJson(JsonElement json) {
        if (!json.isJsonObject()) {
            throw new RuntimeException("JSON 必须是对象");
        }
        JsonObject obj = json.getAsJsonObject();
        String typeId = obj.get("type").getAsString();
        SerDePair serDe = TYPES.get(typeId);
        if (serDe == null) {
            throw new RuntimeException("未知类型：" + typeId);
        }
        return serDe.deserializer.deserialize(obj);
    }

    /**
     * 序列化 IData 为 JSON
     * @param data 数据实例
     * @return JSON 元素
     */
    public static JsonElement toJson(IData data) {
        String typeId = data.getType();
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
