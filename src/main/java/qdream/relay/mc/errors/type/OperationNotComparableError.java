package qdream.relay.mc.errors.type;

import qdream.relay.engine.StateMachine;

/**
 * 操作无法比较
 * <p>翻译键：{@code error.operation_not_comparable}</p>
 */
public class OperationNotComparableError extends TypeException {

    public OperationNotComparableError(StateMachine executor) {
        super(executor, "error.operation_not_comparable");
    }
}
