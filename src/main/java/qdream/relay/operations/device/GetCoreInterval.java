package qdream.relay.operations.device;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;

public class GetCoreInterval extends Instruction {

    public GetCoreInterval() {
        super("relay:get_core_interval", 1, 0.25, OperationSignature.builder()
                .producesToData("interval", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ShellContainer shellContainer = OperationHelpers.getShellContainer(executor);
        executor.pushData(new NumberData(shellContainer.getInterval()));
    }

}
