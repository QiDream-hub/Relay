package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;


import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;

/**
 * Or 操作 - 逻辑或
 */
public class OrOp extends Spell {

    public OrOp() {
        super("relay:or",1, 1, OperationSignature.builder()
                .input("boolean")
                .input("boolean")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof BooleanIota b)) {
            executor.triggerMishap("操作 relay:or 期望 boolean 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof BooleanIota a)) {
            executor.triggerMishap("操作 relay:or 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asBoolean() || b.asBoolean();
        executor.pushData(new BooleanIota(result));
    }

}
