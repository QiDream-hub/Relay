package qdream.relay.operations.device;

import net.minecraft.world.item.ItemStack;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.NumberData;

public class SetCoreInterval extends Instruction {

    public SetCoreInterval() {
        super("relay:set_core_interval", 1, 0.5, OperationSignature.builder()
                .consumesFromData("number", "relay:number")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData popNumber = StackHelpers.popNumber(executor, id);
        ShellContainer shellContainer = OperationHelpers.getShellContainer(executor);
        ItemStack coreStack = shellContainer.getCoreStack();
        if (coreStack.getItem() instanceof ComputingCoreComponent computingCore) {
            executor.pushData(new BooleanData(computingCore.setInterval(coreStack, popNumber.asInt())));
        } else {
            executor.pushData(new BooleanData(false));
        }
    }

}
