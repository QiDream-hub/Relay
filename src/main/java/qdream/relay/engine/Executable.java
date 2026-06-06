package qdream.relay.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 可执行数据接口
 * 可执行的 iota 类型，既存储数据又可执行
 */
public interface Executable extends IData {
    /**
     * 执行此可执行单元
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);

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
         * @return 反序列化后的 Executable
         */
        static Executable fromJson(JsonElement json) {
            return IotaTypeRegistry.fromJson(json);
        }
    }

    /**
     * JSON 序列化器接口
     */
    @FunctionalInterface
    interface JsonElementSerializer {
        JsonElement serialize(Executable data);
    }

    /**
     * JSON 反序列化器接口
     */
    @FunctionalInterface
    interface JsonElementDeserializer {
        Executable deserialize(JsonObject json);
    }
}
