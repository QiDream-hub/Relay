package qdream.relay.operations.communication;

import qdream.relay.types.NumberData;
import qdream.relay.types.BooleanData;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ExecutionException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.core.CommunicationSystem;

/**
 * Send 操作 - 发送数据到频道
 * 弹出：频道号、数据
 */
public class Send extends Instruction {

    public Send() {
        super("relay:send", 1, 0.5, OperationSignature.builder()
                .consumesFromData("channel", "relay:number")
                .consumesFromData("data", "any")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {

        NumberData channel = StackHelpers.popNumber(executor, id);

        Executable data = StackHelpers.popAny(executor);

        int ch = channel.asInt();
        boolean success = CommunicationSystem.send(ch, data);

        if (!success) {
            throw new ExecutionException(executor, "操作 relay:send 频道 " + ch + " 队列已满");
        }

        executor.pushData(new BooleanData(true));
    }

}
