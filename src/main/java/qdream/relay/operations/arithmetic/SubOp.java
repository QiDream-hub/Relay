package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberType;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Sub 操作 - 减法
 */
public class SubOp extends Spell {

    public SubOp() {
        super("relay:sub", 1, 1, OperationSignature.builder()
                .consumesFromData("relay:number")
                .consumesFromData("relay:number")
                .producesToData("relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberType b)) {
            executor.triggerMishap("操作 relay:sub 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberType a)) {
            executor.triggerMishap("操作 relay:sub 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        double result = a.asDouble() - b.asDouble();
        executor.pushData(new NumberType(result));
    }

}
