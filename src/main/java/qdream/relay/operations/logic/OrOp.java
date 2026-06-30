package qdream.relay.operations.logic;

import qdream.relay.types.BooleanType;


import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Or 操作 - 逻辑或
 */
public class OrOp extends Spell {

    public OrOp() {
        super("relay:or",1, 1, OperationSignature.builder()
                .consumesFromData("relay:boolean")
                .consumesFromData("relay:boolean")
                .producesToData("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof BooleanType b)) {
            executor.triggerMishap("操作 relay:or 期望 boolean 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof BooleanType a)) {
            executor.triggerMishap("操作 relay:or 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asBoolean() || b.asBoolean();
        executor.pushData(new BooleanType(result));
    }

}
