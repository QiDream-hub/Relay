package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Not 操作 - 逻辑非
 */
public class NotOp extends Spell {

    public NotOp() {
        super("relay:not", 1, 1, OperationSignature.builder()
                .input("boolean")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof BooleanIota a)) {
            executor.triggerMishap("操作 relay:not 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = !a.asBoolean();
        executor.pushData(new BooleanIota(result));
    }

}
