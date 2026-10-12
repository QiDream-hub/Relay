package qdream.relay.operations.vector;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.type.BlockEntityReferenceInvalidError;
import qdream.relay.mc.errors.type.BlockReferenceInvalidError;
import qdream.relay.mc.errors.entity.EntityReferenceInvalidError;
import qdream.relay.mc.errors.type.ParameterInvalidError;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.TextTools;
import qdream.relay.types.BlockData;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 获取坐标操作
 * 获取方块、方块实体或实体所在的位置坐标
 *
 * 弹出：target (方块/方块实体/实体)
 * 压入：vector (位置坐标)
 *
 * 示例用法：
 * 1. 获取实体坐标：get_self get_pos
 * 2. 获取方块坐标并存储：some_block get_pos some_list list_append
 * 3. 获取方块实体坐标并计算距离：some_block_entity get_pos get_self get_pos vector_distance
 */
public class GetPos extends Instruction {

    public GetPos() {
        super("relay:get_pos", 1, 0.02, OperationSignature.builder()
                .consumesFromData("targetEntity", "relay:block", "relay:block_entity", "relay:entity")
                .producesToData("position", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        Executable targetExe = StackHelpers.popAny(executor, id);

        if (targetExe instanceof EntityData entityData) {
            // 获取实体位置
            var entity = entityData.getEntity();
            if (entity == null) {
                throw new EntityReferenceInvalidError(executor);
            }
            executor.pushData(new VectorData(entity.position()));
        } else if (targetExe instanceof BlockEntityData blockEntityData) {
            // 获取方块实体位置（方块中心）
            if (!blockEntityData.hasPosition()) {
                throw new BlockEntityReferenceInvalidError(executor);
            }
            BlockPos pos = blockEntityData.getBlockPos();
            executor.pushData(new VectorData(Vec3.atCenterOf(pos)));
        } else if (targetExe instanceof BlockData blockData) {
            // 获取方块位置（方块中心）
            if (!blockData.hasPosition()) {
                throw new BlockReferenceInvalidError(executor);
            }
            BlockPos pos = blockData.getBlockPos();
            executor.pushData(new VectorData(Vec3.atCenterOf(pos)));
        } else {
            throw new ParameterInvalidError(executor, TextTools.getName(targetExe));
        }
    }
}
