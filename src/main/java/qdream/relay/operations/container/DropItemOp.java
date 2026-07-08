package qdream.relay.operations.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.SlotData;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

/**
 * 将物品放到指定坐标（以物品实体方式，支持跨维度）
 * 输入：SlotData, BlockEntityData（目标位置）
 * 输出：无
 */
public class DropItemOp extends Spell {

    public DropItemOp() {
        super("relay:drop_item", 1, 1, OperationSignature.builder()
                .consumesFromData("item", "relay:item")
                .consumesFromData("position", "relay:block_entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        BlockEntityData positionData = OperationHelpers.popBlockEntity(executor, id);
        if (positionData == null) {
            return;
        }

        SlotData itemData = OperationHelpers.popSlot(executor, id);
        if (itemData == null) {
            return;
        }

        // 通过 worldId 获取对应的世界（支持跨维度）
        ServerLevel itemLevel = Relay.getWorld(itemData.getWorldId());
        if (itemLevel == null) {
            executor.triggerMishap(id + " 物品世界不存在：" + itemData.getWorldId());
            return;
        }

        ServerLevel positionLevel = Relay.getWorld(positionData.getWorldId());
        if (positionLevel == null) {
            executor.triggerMishap(id + " 目标世界不存在：" + positionData.getWorldId());
            return;
        }

        // 获取目标位置的方块实体（使用目标世界）
        BlockEntity targetBlockEntity = positionData.getBlockEntity(positionLevel);
        if (targetBlockEntity == null) {
            executor.triggerMishap(id + " 目标位置不存在");
            return;
        }

        BlockPos targetPos = targetBlockEntity.getBlockPos();

        // 获取物品堆（使用物品所在世界）
        ItemStack itemStack = itemData.getItemStack(itemLevel);
        if (itemStack.isEmpty()) {
            executor.triggerMishap(id + " 物品不存在");
            return;
        }

        // 在目标位置生成物品实体（使用目标世界）
        ItemEntity entity = ContainerTools.spawnItemEntity(positionLevel, targetPos, itemStack);
        if (entity == null) {
            executor.triggerMishap(id + " 无法生成物品实体");
            return;
        }
    }
}
