package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.VectorData;

import net.minecraft.world.phys.Vec3;

/**
 * 向量归一化操作
 * 将向量转换为单位向量（长度为 1，方向不变）
 *
 * 弹出：vector
 * 压入：vector (归一化后的向量)
 */
public class VectorNormalize extends Instruction {

    public VectorNormalize() {
        super("relay:vector_normalize", 1, 0.05, OperationSignature.builder()
                .consumesFromData("vector", "relay:vector")
                .producesToData("normalized", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData vec = StackHelpers.popVector(executor, id);

        Vec3 v = vec.asVector();
        double length = v.length();

        if (length < 1e-10) {
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ZERO_VECTOR));
        }

        Vec3 result = v.normalize();
        executor.pushData(new VectorData(result));
    }
}
