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
import qdream.relay.Component.RelayDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * 从物品读取列表操作
 * 输入：SlotData（物品引用）
 * 输出：ListData（从磁盘读取的程序列表）
 */
public class SlotToList extends Instruction {

    public SlotToList() {
        super("relay:slot_to_list", 1, 0.5, OperationSignature.builder()
                .consumesFromData("slot", "relay:slot")
                .producesToData("list", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        SlotData slotData = StackHelpers.popSlot(executor, id);

        ItemStack itemStack = slotData.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            throw new ContainerException(executor, id + " 物品为空");
        }

        // 检查物品是否是法术磁盘（检查是否是 DiskComponent 类型）
        if (!(itemStack.getItem() instanceof DiskComponent diskComponent)) {
            throw new ContainerException(executor, id + " 目标物品不是法术磁盘");
        }

        // 从磁盘读取程序列表
        var program = diskComponent.getProgram(itemStack);

        // 将程序列表包装为 ListData 并压入数据栈
        executor.pushData(new ListData(new java.util.ArrayList<>(program)));
    }
}
