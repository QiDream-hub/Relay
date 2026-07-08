package qdream.relay.operations.container;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.SlotData;
import qdream.relay.types.NumberData;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

/**
 * 获取物品数量操作（支持跨维度）
 * 输入：SlotData
 * 输出：NumberData（物品数量）
 */
public class GetItemCountOp extends Spell {

    public GetItemCountOp() {
        super("relay:get_item_count", 1, 0.1, OperationSignature.builder()
                .consumesFromData("item", "relay:slot")
                .producesToData("count", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        SlotData itemData = OperationHelpers.popSlot(executor, id);
        if (itemData == null) {
            return;
        }

        // 通过 worldId 获取对应的世界（支持跨维度）
        ServerLevel level = Relay.getWorld(itemData.getWorldId());
        if (level == null) {
            executor.triggerMishap(id + " 世界不存在：" + itemData.getWorldId());
            return;
        }

        int count = ContainerTools.getItemCount(itemData, level);
        executor.pushData(new NumberData(count));
    }
}
