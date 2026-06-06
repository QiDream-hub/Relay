package qdream.relay.engine;

/**
 * 统一的可执行接口
 */
public interface Executable {

    /**
     * 执行此可执行单元
     *
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);
}