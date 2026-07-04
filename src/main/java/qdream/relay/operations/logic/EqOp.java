package qdream.relay.operations.logic;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanData;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp extends Spell {

    public EqOp() {
        super("relay:eq", 1, 1, OperationSignature.builder()
                .consumesFromData("left", "any")
                .consumesFromData("right", "any")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable b = executor.popData();
        if (b == null) return;
        
        Executable a = executor.popData();
        if (a == null) return;

        boolean result = a.equals(b);
        executor.pushData(new BooleanData(result));
    }

}
