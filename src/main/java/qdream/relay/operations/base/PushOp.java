package qdream.relay.operations.base;

import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * Push 操作 - 从程序栈弹出一个数据值，然后压入数据栈
 * 注意：这个操作由 StateMachine 内部处理，当程序栈弹出的是数据时自动压入数据栈
 * 此操作仅用于签名和编辑器提示
 */
public class PushOp implements StackOperation {
    private final String targetType;

    public PushOp(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 默认构造函数，用于注册表
     */
    public PushOp() {
        this.targetType = "any";
    }

    @Override
    public void execute(StateMachine executor) {
        // Push 操作实际上由 StateMachine 的宽容规则处理
        // 当程序栈弹出数据时，自动压入数据栈
        // 这里不需要额外逻辑
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .output(targetType)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
