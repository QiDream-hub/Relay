package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberData;

/**
 * 获取程序栈长度操作
 * 输入：无
 * 输出：程序栈长度（数值）
 */
public class GetProgramStackLengthOp extends Spell {

    public GetProgramStackLengthOp() {
        super("relay:get_program_stack_length", 0, 1, OperationSignature.builder()
                .producesToData("size", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        int size = executor.getProgramStackSize();
        executor.pushData(new NumberData(size));
    }

}
