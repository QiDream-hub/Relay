package qdream.relay.operations.container;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.SlotData;
import qdream.relay.types.TypeData;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 获取物品类型操作（支持跨维度）
 * 输入：SlotData（物品引用）
 * 输出：TypeData（物品类型）
 */
public class GetItemType extends Instruction {

    public GetItemType() {
        super("relay:get_item_type", 1, 0.1, OperationSignature.builder()
                .consumesFromData("item", "relay:slot")
                .producesToData("type", "relay:type")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        SlotData itemData = StackHelpers.popSlot(executor, id);

        // 获取物品堆并提取类型 ID
        var itemStack = itemData.getItemStack();
        if (itemStack.isEmpty()) {
            throw new ContainerException(executor, id + " 物品不存在");
        }

        // 从物品注册表 ID 获取类型 ID
        String typeId = BuiltInRegistries.ITEM
                .getKey(itemStack.getItem())
                .toString();

        executor.pushData(new TypeData(typeId));
    }
}
