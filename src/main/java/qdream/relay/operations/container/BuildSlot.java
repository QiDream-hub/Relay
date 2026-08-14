package qdream.relay.operations.container;

import qdream.relay.Relay;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.NumberData;
import qdream.relay.types.SlotData;

import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 获取插槽操作
 * 输入：BlockEntityData（方块实体）、NumberData（插槽索引）
 * 输出：SlotData（插槽引用）
 */
public class BuildSlot extends Instruction {

    public BuildSlot() {
        super("relay:build_slot", 1, 0.5, OperationSignature.builder()
                .consumesFromData("container", "relay:block_entity")
                .consumesFromData("slotIndex", "relay:number")
                .producesToData("slotData", "relay:slot")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        BlockEntityData containerData = StackHelpers.popBlockEntity(executor, id);
        NumberData slotIndex = StackHelpers.popNumber(executor, id);

        // 获取方块实体
        BlockEntity blockEntity = containerData.getBlockEntity();
        if (blockEntity == null) {
            throw new ContainerException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorMessageTools.ErrorType.CONTAINER_NOT_FOUND));
        }

        // 检查是否为容器
        if (!(blockEntity instanceof Container container)) {
            throw new ContainerException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorMessageTools.ErrorType.NOT_A_CONTAINER));
        }

        // 获取插槽索引
        int slot = slotIndex.asInt();
        if (slot < 0 || slot >= container.getContainerSize()) {
            throw new ContainerException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorMessageTools.ErrorType.SLOT_NOT_FOUND,
                            slot, container.getContainerSize()));
        }

        // 创建并返回 SlotData
        Level level = Relay.getWorld(containerData.getWorldId());
        SlotData slotData = SlotData.from(
                blockEntity.getBlockPos(),
                level,
                slot);

        executor.pushData(slotData);
    }
}
