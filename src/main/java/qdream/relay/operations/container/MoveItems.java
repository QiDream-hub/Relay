package qdream.relay.operations.container;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.NumberData;
import qdream.relay.types.SlotData;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

/**
 * 移动物品操作（支持任意两个容器的物品移动，跨维度支持）
 * 尝试将 source_item 移动到 target_item（堆叠或放入空槽）
 * 输入：SlotData（目标容器/槽位），SlotData（源物品）
 * 输出：NumberData（source_item 剩余的数量，0 表示完全移动）
 */
public class MoveItems extends Instruction {

    public MoveItems() {
        super("relay:move_items", 2, 0.25, OperationSignature.builder()
                .consumesFromData("target_item", "relay:slot")
                .consumesFromData("source_item", "relay:slot")
                .producesToData("remaining", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        SlotData sourceItem = StackHelpers.popSlot(executor, id);
        if (sourceItem == null) {
            return;
        }

        SlotData targetItem = StackHelpers.popSlot(executor, id);
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

        // 获取源物品堆
        ItemStack sourceStack = sourceContainer.getItem(sourceItem.getSlot());
        if (sourceStack.isEmpty()) {
            executor.pushData(new NumberData(0));
            return;
        }

        // 执行物品移动（支持跨容器、跨维度）
        int remaining = ContainerTools.moveItems(
                targetContainer,
                targetItem.getSlot(),
                sourceContainer,
                sourceItem.getSlot());

        // 返回剩余的物品数量
        executor.pushData(new NumberData(remaining));
    }
}
