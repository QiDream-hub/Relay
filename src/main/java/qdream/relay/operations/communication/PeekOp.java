package qdream.relay.operations.communication;

import qdream.relay.types.NumberType;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.core.CommunicationSystem;

/**
 * Peek 操作 - 窥探数据（不出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class PeekOp extends Spell {

    public PeekOp() {
        super("relay:peek", 1, 1, OperationSignature.builder()
                .consumesFromData("relay:number")
                .producesToData("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation channelData = (Operation) executor.popData();
        if (channelData == null) return;
        if (!(channelData instanceof NumberType channel)) {
            executor.triggerMishap("操作 relay:peek 期望 number 类型，实际为：" + channelData.getId());
            return;
        }

        int ch = channel.asInt();
        Executable data = CommunicationSystem.peek(ch);
        executor.pushData(data);
    }

}
