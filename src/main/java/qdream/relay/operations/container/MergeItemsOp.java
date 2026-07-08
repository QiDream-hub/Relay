package qdream.relay.operations.container;

import net.minecraft.world.Container;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.BooleanData;
import qdream.relay.types.SlotData;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

/**
 * 合并两个物品操作（支持任意两个容器的物品堆叠，跨维度支持）
 * 输入：SlotData（目标物品），SlotData（源物品）
 * 输出：BooleanData（完全合并返回 true），如果部分合并则返回剩余的 SlotData
 */
public class MergeItemsOp extends Spell {

    public MergeItemsOp() {
        super("relay:merge_items", 2, 0.25, OperationSignature.builder()
                .consumesFromData("target_item", "relay:slot")
                .consumesFromData("source_item", "relay:slot")
                .producesToData("result", "boolean|relay:item")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        SlotData sourceItem = OperationHelpers.popSlot(executor, id);
        if (sourceItem == null) {
            return;
        }

        SlotData targetItem = OperationHelpers.popSlot(executor, id);
        if (targetItem == null) {
            return;
        }

        // 通过 worldId 获取对应的世界（支持跨维度合并）
        ServerLevel targetLevel = Relay.getWorld(targetItem.getWorldId());
        if (targetLevel == null) {
            executor.triggerMishap(id + " 目标世界不存在：" + targetItem.getWorldId());
            return;
        }

        ServerLevel sourceLevel = Relay.getWorld(sourceItem.getWorldId());
        if (sourceLevel == null) {
            executor.triggerMishap(id + " 源世界不存在：" + sourceItem.getWorldId());
            return;
        }

        // 通过位置获取目标容器
        var targetBlockEntity = targetLevel.getBlockEntity(targetItem.getContainerPos());
        if (targetBlockEntity == null) {
            executor.triggerMishap(id + " 目标容器不存在");
            return;
        }

        if (!(targetBlockEntity instanceof Container targetContainer)) {
            executor.triggerMishap(id + " 目标不是容器");
            return;
        }

        // 通过位置获取源容器
        var sourceBlockEntity = sourceLevel.getBlockEntity(sourceItem.getContainerPos());
        if (sourceBlockEntity == null) {
            executor.triggerMishap(id + " 源容器不存在");
            return;
        }

        if (!(sourceBlockEntity instanceof Container sourceContainer)) {
            executor.triggerMishap(id + " 源不是容器");
            return;
        }

        // 执行合并（支持跨容器、跨维度）
        ContainerTools.MergeResult result = ContainerTools.tryMergeItems(
                targetContainer,
                targetItem.getContainerPos(),
                targetItem.getSlot(),
                sourceContainer,
                sourceItem.getContainerPos(),
                sourceItem.getSlot(),
                targetLevel);

        if (result.fullyMerged()) {
            executor.pushData(new BooleanData(true));
        } else {
            // 返回剩余的物品
            if (result.remaining() != null) {
                executor.pushData(result.remaining());
            } else {
                executor.pushData(new BooleanData(false));
            }
        }
    }
}
