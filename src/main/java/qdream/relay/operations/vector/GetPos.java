package qdream.relay.operations.vector;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.TypeException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.TextTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
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
                throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ENTITY_REFERENCE_INVALID));
            }
            executor.pushData(new VectorData(entity.position()));
        } else if (targetExe instanceof BlockEntityData blockEntityData) {
            // 获取方块实体位置（方块中心）
            if (!blockEntityData.hasPosition()) {
                throw new TypeException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.BLOCK_ENTITY_REFERENCE_INVALID));
            }
            BlockPos pos = blockEntityData.getBlockPos();
            executor.pushData(new VectorData(Vec3.atCenterOf(pos)));
        } else if (targetExe instanceof BlockData blockData) {
            // 获取方块位置（方块中心）
            if (!blockData.hasPosition()) {
                throw new TypeException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.BLOCK_REFERENCE_INVALID));
            }
            BlockPos pos = blockData.getBlockPos();
            executor.pushData(new VectorData(Vec3.atCenterOf(pos)));
        } else {
            throw new TypeException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.PARAMETER_INVALID, TextTools.getName(targetExe)));
        }
    }
}
