package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;


/**
 * 向量长度计算操作
 *
 * 弹出：vector
 * 压入：number (向量的模长)
 */
public class VectorLength extends Instruction {

    public VectorLength() {
        super("relay:vector_length", 1, 0.05, OperationSignature.builder()
                .consumesFromData("vector", "relay:vector")
                .producesToData("length", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData vec = StackHelpers.popVector(executor, id);

        double length = vec.asVector().length();
        executor.pushData(new NumberData(length));
    }
}
