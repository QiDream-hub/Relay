package qdream.relay.operations.vector;

import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * VectorSplit 操作 - 将向量拆分为三个数字
 */
public class VectorSplit extends Instruction {

    public VectorSplit() {
        super("relay:vector_split", 1, 0.25, OperationSignature.builder()
                .consumesFromData("vector", "relay:vector")
                .producesToData("x", "relay:number")
                .producesToData("y", "relay:number")
                .producesToData("z", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData vectorData = StackHelpers.popVector(executor, id);
        if (vectorData == null) return;

        double x = vectorData.asVector().x;
        double y = vectorData.asVector().y;
        double z = vectorData.asVector().z;

        executor.pushData(new NumberData(z));
        executor.pushData(new NumberData(y));
        executor.pushData(new NumberData(x));
    }

}
