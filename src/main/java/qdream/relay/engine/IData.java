package qdream.relay.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 数据接口
 * 所有可存储在栈中的数据类型必须实现此接口
 * 
 * @deprecated 所有类型应直接实现 {@link Executable}
 */
@Deprecated
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
}
