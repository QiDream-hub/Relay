package qdream.relay.operations.container;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.ContainerHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.SlotData;
import qdream.relay.types.ListData;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

import java.util.List;

/**
 * 获取容器内的物品操作（支持跨维度）
 * 输入：BlockEntityData（容器）
 * 输出：ListData（包含 SlotData 的列表）
 */
public class GetContainerItems extends Instruction {

    public GetContainerItems() {
        super("relay:get_container_items", 1, 0.5, OperationSignature.builder()
                .consumesFromData("container", "relay:block_entity")
                .producesToData("items", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        BlockEntityData containerData = StackHelpers.popBlockEntity(executor, id);

        BlockEntity blockEntity = containerData.getBlockEntity();
        if (blockEntity == null) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.CONTAINER_NOT_FOUND));
        }

        if (!(blockEntity instanceof Container container)) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.NOT_A_CONTAINER));
        }

        // 通过 worldId 获取对应的世界（支持跨维度）
        ServerLevel level = Relay.getWorld(containerData.getWorldId());
        if (level == null) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.WORLD_NOT_FOUND, containerData.getWorldId()));
        }
        List<SlotData> items = ContainerHelpers.getContainerItems(
                container,
                blockEntity.getBlockPos(),
                level);

        executor.pushData(new ListData(new java.util.ArrayList<>(items)));
    }
}
