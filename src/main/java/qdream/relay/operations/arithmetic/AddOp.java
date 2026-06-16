package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberIota;
// import qdream.relay.Relay;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Add 操作 - 加法
 */
public class AddOp extends Spell {

    public AddOp() {
        super("relay:add", 1, 1, OperationSignature.builder()
                .input("number")
                .input("number")
                .output("relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation bData = (Operation) executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberIota b)) {
            executor.triggerMishap("操作 relay:add 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Operation aData = (Operation) executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:add 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        double result = a.asDouble() + b.asDouble();
        // Relay.LOGGER.info("AddOp: " + result);
        executor.pushData(new NumberIota(result));
    }

}
