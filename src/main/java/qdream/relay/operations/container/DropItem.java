package qdream.relay.operations.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
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
public class DropItem extends Instruction {

    public DropItem() {
        super("relay:drop_item", 1, 1, OperationSignature.builder()
                .consumesFromData("item", "relay:slot")
                .consumesFromData("position", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        VectorData positionData = StackHelpers.popVector(executor, id);
        SlotData itemData = StackHelpers.popSlot(executor, id);

        ServerLevel positionLevel = Relay.getWorld(itemData.getWorldId());
        if (positionLevel == null) {
            throw new ContainerException(id + " 目标世界不存在：" + itemData.getWorldId());
        }

        // 从向量获取目标位置
        Vec3 targetVec = positionData.asVector();
        BlockPos targetPos = BlockPos.containing(targetVec);

        // 获取物品堆（使用物品所在世界）
        ItemStack itemStack = itemData.getItemStack();
        if (itemStack.isEmpty()) {
            throw new ContainerException(id + " 物品不存在");
        }

        // 在目标位置生成物品实体（使用目标世界）
        ItemEntity entity = ContainerTools.spawnItemEntity(positionLevel, targetPos, itemStack);
        if (entity == null) {
            throw new ContainerException(id + " 无法生成物品实体");
        }
    }
}
