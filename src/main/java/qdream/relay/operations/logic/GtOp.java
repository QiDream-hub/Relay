package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;

/**
 * Gt 操作 - 大于比较
 */
public class GtOp extends Spell {

    public GtOp() {
        super("relay:gt", 1, 1, OperationSignature.builder()
                .input("number")
                .input("number")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberIota b)) {
            executor.triggerMishap("操作 relay:gt 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:gt 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asDouble() > b.asDouble();
        executor.pushData(new BooleanIota(result));
    }

}
