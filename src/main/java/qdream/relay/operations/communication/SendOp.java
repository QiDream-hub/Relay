package qdream.relay.operations.communication;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;
import qdream.relay.core.CommunicationSystem;

/**
 * Send 操作 - 发送数据到频道
 * 弹出：频道号、数据
 */
public class SendOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData dataData = executor.popData();
        if (!(dataData instanceof McIota data)) return;
        IData channelData = executor.popData();
        if (!(channelData instanceof McIota channel)) return;
        
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
        
        executor.pushData(McIota.ofBoolean(true));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("number")
                .input("any")
                .output("boolean")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
