package qdream.relay.operations.communication;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.core.CommunicationSystem;

/**
 * Recv 操作 - 接收数据（出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class RecvOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota channel = executor.popData();
        
        if (channel == null) {
            return;
        }
        
        if (!channel.isNumber()) {
            throw new IllegalArgumentException("Recv 需要数值型频道号");
        }
        
        int ch = channel.asInt();
        Iota data = CommunicationSystem.recv(ch);
        executor.pushData(data);
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.NUMBER)
                .output(IotaType.ANY)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
