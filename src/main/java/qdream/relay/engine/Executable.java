package qdream.relay.engine;

/**
 * 统一的可执行接口
 */
public interface Executable {
    /**
     * 获取唯一 ID
     * 数据："relay:number", "relay:boolean"...
     * 操作："relay:add", "relay:sub"...
     */
    String getId();

    /**
     * 执行此可执行单元
     *
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);
}