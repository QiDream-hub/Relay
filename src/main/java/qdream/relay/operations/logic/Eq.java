package qdream.relay.operations.logic;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.TypeException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;

/**
 * Eq 操作 - 等于比较
 */
public class Eq extends Instruction {

    public Eq() {
        super("relay:eq", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "any")
                .consumesFromData("right", "any")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable a = StackHelpers.popAny(executor, id);
        Executable b = StackHelpers.popAny(executor, id);

        if (!(a instanceof Operation operationA && b instanceof Operation operationB)) {
            throw new TypeException(
                executor,
                ErrorMessageTools.buildErrorMessage(ErrorType.OPERATION_NOT_COMPARABLE)
            );
        }

        executor.pushData(new BooleanData(operationA.equalsTo(operationB)));
    }

}
