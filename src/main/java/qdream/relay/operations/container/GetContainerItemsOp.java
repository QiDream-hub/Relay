package qdream.relay.operations.container;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
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
public class GetContainerItemsOp extends Spell {

    public GetContainerItemsOp() {
        super("relay:get_container_items", 1, 0.5, OperationSignature.builder()
                .consumesFromData("container", "relay:block_entity")
                .producesToData("items", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        BlockEntityData containerData = OperationHelpers.popBlockEntity(executor, id);
        if (containerData == null) {
            return;
        }

        // 通过 worldId 获取对应的世界（支持跨维度）
        ServerLevel level = Relay.getWorld(containerData.getWorldId());
        if (level == null) {
            executor.triggerMishap(id + " 世界不存在：" + containerData.getWorldId());
            return;
        }

        BlockEntity blockEntity = containerData.getBlockEntity(level);
        if (blockEntity == null) {
            executor.triggerMishap(id + " 容器不存在");
            return;
        }

        if (!(blockEntity instanceof Container container)) {
            executor.triggerMishap(id + " 目标不是容器");
            return;
        }

        List<SlotData> items = ContainerTools.getContainerItems(
                container,
                blockEntity.getBlockPos(),
                level
        );

        executor.pushData(new ListData(new java.util.ArrayList<>(items)));
    }
}
