package qdream.relay.operations.communication;

import qdream.relay.types.NumberIota;
import qdream.relay.types.BooleanIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

import qdream.relay.core.CommunicationSystem;

/**
 * Send 操作 - 发送数据到频道
 * 弹出：频道号、数据
 */
public class SendOp extends Spell {

    protected SendOp() {
        super("relay:send", 1, OperationSignature.builder()
                .input("number")
                .input("any")
                .output("boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable dataData = executor.popData();
        if (dataData == null) return;
        Executable channelData = executor.popData();
        if (channelData == null) return;
        if (!(channelData instanceof NumberIota channel)) {
            executor.triggerMishap("操作 relay:send 期望 number 类型，实际为：" + channelData.getId());
            return;
        }

        int ch = channel.asInt();
        boolean success = CommunicationSystem.send(ch, dataData);

        if (!success) {
            executor.triggerMishap("操作 relay:send 频道 " + ch + " 队列已满");
            return;
        }

        executor.pushData(new BooleanIota(true));
    }

}
