package qdream.relay.operations.communication;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;
import qdream.relay.core.CommunicationSystem;

/**
 * Send 操作 - 发送数据到频道
 * 弹出：频道号、数据
 */
public class SendOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota data = executor.popData();
        Iota channel = executor.popData();
        
        if (data == null || channel == null) {
            return;
        }
        
        if (!channel.isNumber()) {
            throw new IllegalArgumentException("Send 需要数值型频道号");
        }
        
        int ch = channel.asInt();
        boolean success = CommunicationSystem.send(ch, data);
        
        if (!success) {
            throw new IllegalStateException("频道 " + ch + " 队列已满");
        }
        
        executor.pushData(Iota.ofBoolean(true));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.NUMBER)
                .input(IotaType.ANY)
                .output(IotaType.BOOLEAN)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
