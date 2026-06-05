package qdream.relay.engine;

/**
 * 可执行数据接口
 * 可执行的 iota 类型，既存储数据又可执行
 */
public interface IExecutable extends IData {
    /**
     * 执行此可执行单元
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);
}
