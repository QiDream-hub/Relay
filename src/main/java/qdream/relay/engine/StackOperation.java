package qdream.relay.engine;

/**
 * 操作接口
 * 所有操作必须实现此接口
 */
public interface StackOperation {
    /**
     * 执行操作
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);

    /**
     * 获取操作签名（可选，用于类型推导）
     */
    default OperationSignature getSignature() {
        return OperationSignature.builder().build();
    }

    /**
     * 操作消耗的操作数（默认 1）
     */
    default int getCost() {
        return 1;
    }
}
