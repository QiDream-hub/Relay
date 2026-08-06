package qdream.relay.operations.container;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.ListData;
import qdream.relay.types.SlotData;
import qdream.relay.mc.component.DiskComponent;

import net.minecraft.world.item.ItemStack;

/**
 * 向物品写入列表操作
 * 输入：SlotData（物品引用）、ListData（程序列表）
 * 输出：无
 */
public class ListToSlot extends Instruction {

    public ListToSlot() {
        super("relay:list_to_slot", 2, 1.0, OperationSignature.builder()
                .consumesFromData("slot", "relay:slot")
                .consumesFromData("list", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        ListData listData = StackHelpers.popList(executor, id);
        SlotData slotData = StackHelpers.popSlot(executor, id);

        ItemStack itemStack = slotData.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            throw new ContainerException(executor, id + " 物品为空");
        }

        // 检查物品是否是法术磁盘（检查是否是 DiskComponent 类型）
        if (!(itemStack.getItem() instanceof DiskComponent diskComponent)) {
            throw new ContainerException(executor, id + " 目标物品不是法术磁盘");
        }

        // 将 ListData 中的程序列表写入磁盘
        diskComponent.setProgram(itemStack, listData.getValue());
    }
}
