package qdream.relay.operations.communication;

import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.core.CommunicationSystem;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;

/**
 * Recv 操作 - 接收数据（出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class RecvOp extends Spell {

    public RecvOp() {
        super("relay:recv", 1, 0.25, OperationSignature.builder()
                .consumesFromData("channel", "relay:number")
                .producesToData("data", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData channel = OperationHelpers.popNumber(executor, id);
        if (channel == null) return;

        int ch = channel.asInt();
        Executable data = CommunicationSystem.recv(ch);
        executor.pushData(data);
    }

}
