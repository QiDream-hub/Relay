package qdream.relay.operations.container;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.SlotData;
import qdream.relay.types.NumberData;

/**
 * 获取物品数量操作（支持跨维度）
 * 输入：SlotData
 * 输出：NumberData（物品数量）
 */
public class GetItemCount extends Instruction {

    public GetItemCount() {
        super("relay:get_item_count", 1, 0.1, OperationSignature.builder()
                .consumesFromData("item", "relay:slot")
                .producesToData("count", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        SlotData itemData = StackHelpers.popSlot(executor, id);

        int count = ContainerTools.getItemCount(itemData);
        executor.pushData(new NumberData(count));
    }
}
