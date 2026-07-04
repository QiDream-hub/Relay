package qdream.relay.operations.communication;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.core.CommunicationSystem;

/**
 * Recv 操作 - 接收数据（出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class RecvOp extends Spell {

    public RecvOp() {
        super("relay:recv", 1, 1, OperationSignature.builder()
                .consumesFromData("channel", "relay:number")
                .producesToData("data", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation channelData = (Operation) executor.popData();
        if (channelData == null) return;
        if (!(channelData instanceof NumberData channel)) {
            executor.triggerMishap("操作 relay:recv 期望 number 类型，实际为：" + channelData.getId());
            return;
        }

        int ch = channel.asInt();
        Executable data = CommunicationSystem.recv(ch);
        executor.pushData(data);
    }

}
