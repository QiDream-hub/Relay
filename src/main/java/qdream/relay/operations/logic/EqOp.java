package qdream.relay.operations.logic;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp extends Spell {

    public EqOp() {
        super("relay:eq", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "any")
                .consumesFromData("right", "any")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable a = StackHelpers.popAny(executor);
        Executable b = StackHelpers.popAny(executor);

        if (!(a instanceof Operation operationA && b instanceof Operation operationB)) {
            executor.triggerMishap("未知操作无法比较");
            return;
        }

        executor.pushData(new BooleanData(operationA.equalsTo(operationB)));
    }

}
