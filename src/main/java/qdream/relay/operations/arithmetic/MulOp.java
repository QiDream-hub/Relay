package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;

/**
 * Mul 操作 - 乘法
 */
public class MulOp extends Spell {

    public MulOp() {
        super("relay:mul", 1, OperationSignature.builder()
                .input("number")
                .input("number")
                .output("number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberIota b)) {
            executor.triggerMishap("操作 relay:mul 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:mul 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        double result = a.asDouble() * b.asDouble();
        executor.pushData(new NumberIota(result));
    }

}
