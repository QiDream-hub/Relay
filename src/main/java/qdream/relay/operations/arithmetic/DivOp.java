package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Div 操作 - 除法
 */
public class DivOp extends Spell {

    public DivOp() {
        super("relay:div", 1, 1, OperationSignature.builder()
                .consumesFromData("dividend", "relay:number")
                .consumesFromData("divisor", "relay:number")
                .producesToData("quotient", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberData b)) {
            executor.triggerMishap("操作 relay:div 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberData a)) {
            executor.triggerMishap("操作 relay:div 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        double divisor = b.asDouble();
        if (divisor == 0) {
            executor.triggerMishap("操作 relay:div 除零错误");
            return;
        }

        double result = a.asDouble() / divisor;
        executor.pushData(new NumberData(result));
    }

}
