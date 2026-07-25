package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

import net.minecraft.world.phys.Vec3;

/**
 * 向量数乘操作
 *
 * 弹出：number, vector
 * 压入：vector (向量 * 标量)
 */
public class VectorMulOp extends Spell {

    public VectorMulOp() {
        super("relay:vector_mul", 1, 0.05, OperationSignature.builder()
                .consumesFromData("scalar", "relay:number")
                .consumesFromData("vector", "relay:vector")
                .producesToData("result", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData vec = StackHelpers.popVector(executor, id);
        if (vec == null)
            return;

        NumberData num = StackHelpers.popNumber(executor, id);
        if (num == null)
            return;

        double scalar = num.asDouble();
        Vec3 result = vec.asVector().scale(scalar);
        executor.pushData(new VectorData(result));
    }
}
