package qdream.relay.engine;

/**
 * 统一的可执行接口
 * engine 层保持最小化，只负责纯粹的执行
 */
public interface Executable {

    /**
     * 执行此可执行单元
     *
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);
}