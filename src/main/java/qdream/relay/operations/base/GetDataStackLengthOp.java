package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberData;

/**
 * 获取数据栈长度操作
 * 输入：无
 * 输出：数据栈长度（数值）
 */
public class GetDataStackLengthOp extends Instruction {

    public GetDataStackLengthOp() {
        super("relay:get_data_stack_length", 1, 0.25, OperationSignature.builder()
                .producesToData("size", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        int size = executor.getDataStackSize();
        executor.pushData(new NumberData(size));
    }

}
