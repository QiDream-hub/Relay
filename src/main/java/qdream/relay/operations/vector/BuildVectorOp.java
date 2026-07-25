package qdream.relay.operations.vector;

import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

import net.minecraft.world.phys.Vec3;

/**
 * BuildVector 操作 - 从三个数字构建向量
 */
public class BuildVectorOp extends Spell {

    public BuildVectorOp() {
        super("relay:build_vector", 1, 0.05, OperationSignature.builder()
                .consumesFromData("x", "relay:number")
                .consumesFromData("y", "relay:number")
                .consumesFromData("z", "relay:number")
                .producesToData("vector", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData z = StackHelpers.popNumber(executor, id);
        if (z == null) return;

        NumberData y = StackHelpers.popNumber(executor, id);
        if (y == null) return;

        NumberData x = StackHelpers.popNumber(executor, id);
        if (x == null) return;

        Vec3 vector = new Vec3(x.asDouble(), y.asDouble(), z.asDouble());
        executor.pushData(new VectorData(vector));
    }

}
