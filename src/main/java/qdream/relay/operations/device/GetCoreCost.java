package qdream.relay.operations.device;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;

public class GetCoreCost extends Instruction {

    public GetCoreCost() {
        super("relay:get_core_cost", 1, 0.25, OperationSignature.builder()
                .producesToData("count", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ShellContainer shellContainer = OperationHelpers.getShellContainer(executor);
        executor.pushData(new NumberData(shellContainer.getCoreCost()));
    }

}
