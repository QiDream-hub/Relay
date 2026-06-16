package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp extends Spell {

    public EqOp() {
        super("relay:eq", 1, 1, OperationSignature.builder()
                .input("any")
                .input("any")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null)
            return;
        Executable aData = executor.popData();
        if (aData == null)
            return;

        boolean result = aData.equals(bData);
        executor.pushData(new BooleanIota(result));
    }

}
