package qdream.relay.operations.disk;

import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.ListData;
import qdream.relay.types.SlotData;
import qdream.relay.mc.component.DiskComponent;

/**
 * 向磁盘写入程序操作
 * 将程序列表保存到法术磁盘中
 *
 * 弹出：slot (物品引用), list (程序列表)
 * 压入：无
 *
 * 示例用法：
 * 1. 写入磁盘：program_list disk_slot write_disk
 * 2. 创建并保存：build_program some_list disk_slot write_disk
 * 3. 备份程序：disk_slot read_disk some_list list_append other_disk write_disk
 */
public class WriteDisk extends Instruction {

    public WriteDisk() {
        super("relay:write_disk", 2, 1.0, OperationSignature.builder()
                .consumesFromData("slot", "relay:slot")
                .consumesFromData("program", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        SlotData slotData = StackHelpers.popSlot(executor, id);
        ListData listData = StackHelpers.popList(executor, id);

        ItemStack itemStack = slotData.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ITEM_NOT_FOUND));
        }

        // 检查物品是否是法术磁盘
        if (!(itemStack.getItem() instanceof DiskComponent diskComponent)) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.NOT_A_SPELL_DISK));
        }

        // 将 ListData 中的程序列表写入磁盘
        java.util.List<Executable> program = listData.getValue();
        String programJson = ProgramCompiler.toJsonString(program);
        diskComponent.setProgram(itemStack, programJson);
    }
}
