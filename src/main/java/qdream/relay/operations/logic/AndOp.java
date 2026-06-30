package qdream.relay.operations.logic;

import qdream.relay.types.BooleanType;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * And 操作 - 逻辑与
 */
public class AndOp extends Spell {

    public AndOp() {
        super("relay:and", 1, 1, OperationSignature.builder()
                .consumesFromData("boolean")
                .consumesFromData("boolean")
                .producesToData("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof BooleanType b)) {
            executor.triggerMishap("操作 relay:and 期望 boolean 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof BooleanType a)) {
            executor.triggerMishap("操作 relay:and 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asBoolean() && b.asBoolean();
        executor.pushData(new BooleanType(result));
    }

}
