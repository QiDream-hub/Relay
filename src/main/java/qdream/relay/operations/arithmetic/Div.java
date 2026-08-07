package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;

/**
 * Div 操作 - 除法
 */
public class Div extends Instruction {

    public Div() {
        super("relay:div", 1, 0.05, OperationSignature.builder()
                .consumesFromData("dividend", "relay:number")
                .consumesFromData("divisor", "relay:number")
                .producesToData("quotient", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = StackHelpers.popNumber(executor, id);
        NumberData a = StackHelpers.popNumber(executor, id);

        double divisor = b.asDouble();
        if (divisor == 0) {
            throw new ParameterException(
                executor,
                ErrorMessageTools.buildErrorMessage(ErrorType.DIVISION_BY_ZERO)
            );
        }

        double result = a.asDouble() / divisor;
        executor.pushData(new NumberData(result));
    }

}
