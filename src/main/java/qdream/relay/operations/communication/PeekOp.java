package qdream.relay.operations.communication;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;
import qdream.relay.core.CommunicationSystem;

/**
 * Peek 操作 - 窥探数据（不出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class PeekOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData channelData = executor.popData();
        if (!(channelData instanceof McIota channel)) return;
        
        if (channel == null) {
            return;
        }
        
        if (!channel.isNumber()) {
            throw new IllegalArgumentException("Peek 需要数值型频道号");
        }
        
        int ch = channel.asInt();
        McIota data = CommunicationSystem.peek(ch);
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
