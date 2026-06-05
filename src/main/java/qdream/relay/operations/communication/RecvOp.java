package qdream.relay.operations.communication;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;
import qdream.relay.core.CommunicationSystem;

/**
 * Recv 操作 - 接收数据（出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class RecvOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData channelData = executor.popData();
        if (!(channelData instanceof McIota channel)) return;
        
        if (channel == null) {
            return;
        }
        
        if (!channel.isNumber()) {
            throw new IllegalArgumentException("Recv 需要数值型频道号");
        }
        
        int ch = channel.asInt();
        McIota data = CommunicationSystem.recv(ch);
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
