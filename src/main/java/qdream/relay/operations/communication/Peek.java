package qdream.relay.operations.communication;

import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.core.CommunicationSystem;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;

/**
 * Peek 操作 - 窥探数据（不出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class Peek extends Instruction {

    public Peek() {
        super("relay:peek", 1, 0.5, OperationSignature.builder()
                .consumesFromData("channel", "relay:number")
                .producesToData("data", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData channel = StackHelpers.popNumber(executor, id);

        int ch = channel.asInt();
        Executable data = CommunicationSystem.peek(ch);
        executor.pushData(data);
    }

}
