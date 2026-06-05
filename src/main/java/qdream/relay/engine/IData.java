package qdream.relay.engine;

/**
 * 数据接口
 * 所有可存储在栈中的数据类型必须实现此接口
 */
public interface IData {
    /**
     * 获取数据类型标识符
     * 返回字符串而非枚举，使 engine 包不依赖具体类型定义
     */
    String getType();

    /**
     * 获取原始值
     */
    Object getValue();
}
