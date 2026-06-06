package qdream.relay.engine;

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
     * 执行此可执行单元
     * 
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);

    /**
     * 序列化为 JSON
     * 
     * @return JSON 元素
     */
    JsonObject toJson();

    /**
     * 从 JSON 反序列化
     * 
     * @param json JSON 元素，必须包含 "id" 字段
     * @return 反序列化后的 Executable
     */
    Executable fromJson(JsonObject json);
}
