package qdream.relay.operations.communication;

import qdream.relay.engine.Executable;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.core.CommunicationSystem;

/**
 * Peek 操作 - 窥探数据（不出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class PeekOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable channelData = executor.popData();
        if (channelData == null) return;
        if (!(channelData instanceof NumberIota channel)) {
            executor.triggerMishap("操作 relay:peek 期望 number 类型，实际为：" + channelData.getId());
            return;
        }

        int ch = channel.asInt();
        Executable data = CommunicationSystem.peek(ch);
        executor.pushData(data);
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("number")
                .output("any")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
