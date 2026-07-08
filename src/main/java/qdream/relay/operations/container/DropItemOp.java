package qdream.relay.operations.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.SlotData;
import qdream.relay.types.VectorData;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

/**
 * 在指定坐标生成物品实体（支持跨维度）
 * 输入：SlotData（物品引用）, VectorData（目标位置）
 * 输出：无
 */
public class DropItemOp extends Spell {

    public DropItemOp() {
        super("relay:drop_item", 1, 1, OperationSignature.builder()
                .consumesFromData("item", "relay:slot")
                .consumesFromData("position", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        VectorData positionData = OperationHelpers.popVector(executor, id);
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

        ServerLevel positionLevel = Relay.getWorld(itemData.getWorldId());
        if (positionLevel == null) {
            executor.triggerMishap(id + " 目标世界不存在：" + itemData.getWorldId());
            return;
        }

        // 从向量获取目标位置
        Vec3 targetVec = positionData.asVector();
        BlockPos targetPos = BlockPos.containing(targetVec);

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
