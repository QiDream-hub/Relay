package qdream.relay.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 数据接口
 * 所有可存储在栈中的数据类型必须实现此接口
 */
public interface IData {
    /**
     * 获取数据类型标识符
     * @return 类型 ID，如 "relay:number"
     */
    String getType();

    /**
     * 获取原始值
     */
    Object getValue();

    /**
     * 序列化为 JSON
     * @return JSON 元素
     */
    JsonElement toJson();

    /**
     * 类型注册表接口
     * 用于注册类型的 JSON 序列化/反序列化器
     */
    interface TypeRegistry {
        /**
         * 注册类型
         * @param typeId 类型 ID
         * @param serializer 序列化器
         * @param deserializer 反序列化器
         */
        static void register(String typeId, JsonElementSerializer serializer, JsonElementDeserializer deserializer) {
            IotaTypeRegistry.register(typeId, serializer, deserializer);
        }

        /**
         * 从 JSON 反序列化
         * @param json JSON 元素
         * @return 反序列化后的 IData
         */
        static IData fromJson(JsonElement json) {
            return IotaTypeRegistry.fromJson(json);
        }
    }

    /**
     * JSON 序列化器接口
     */
    @FunctionalInterface
    interface JsonElementSerializer {
        JsonElement serialize(IData data);
    }

    /**
     * JSON 反序列化器接口
     */
    @FunctionalInterface
    interface JsonElementDeserializer {
        IData deserialize(JsonObject json);
    }
}
