package qdream.relay.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 统一的可执行接口
 * 所有数据类型和操作都实现此接口
 */
public interface Executable {
    /**
     * 获取唯一 ID
     * 数据："relay:number", "relay:boolean", "relay:string"...
     * 操作："relay:add", "relay:sub", "relay:place_block"...
     */
    String getId();
    
    /**
     * 获取原始值（用于调试显示）
     * @return 原始值
     */
    Object getValue();

    /**
     * 执行此可执行单元
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);

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
         * @param id 类型 ID
         * @param serializer 序列化器
         * @param deserializer 反序列化器
         */
        static void register(String id, JsonElementSerializer serializer, JsonElementDeserializer deserializer) {
            IotaTypeRegistry.register(id, serializer, deserializer);
        }

        /**
         * 从 JSON 反序列化
         * @param json JSON 元素，必须包含 "id" 字段
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
