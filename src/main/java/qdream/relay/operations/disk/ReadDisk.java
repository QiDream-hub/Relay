package qdream.relay.operations.disk;

import java.util.ArrayList;

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
 * 从磁盘读取程序操作
 * 从法术磁盘中读取存储的程序列表
 *
 * 弹出：slot (物品引用)
 * 压入：list (程序列表)
 *
 * 示例用法：
 * 1. 读取磁盘程序：disk_slot read_disk
 * 2. 读取并执行：disk_slot read_disk eval
 * 3. 读取并检查大小：disk_slot read_disk list_length
 */
public class ReadDisk extends Instruction {

    public ReadDisk() {
        super("relay:read_disk", 1, 0.5, OperationSignature.builder()
                .consumesFromData("slot", "relay:slot")
                .producesToData("program", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        SlotData slotData = StackHelpers.popSlot(executor, id);

        ItemStack itemStack = slotData.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ITEM_NOT_FOUND));
        }

        // 检查物品是否是法术磁盘
        if (!(itemStack.getItem() instanceof DiskComponent diskComponent)) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.NOT_A_SPELL_DISK));
        }

        // 从磁盘读取程序列表
        String programJson = diskComponent.getProgram(itemStack);
        if (programJson == null || programJson.trim().isEmpty()) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.DISK_EMPTY));
        }

        try {
            java.util.List<Executable> program = ProgramCompiler.compileFromJson(programJson);
            executor.pushData(new ListData(new ArrayList<>(program)));
        } catch (ProgramCompiler.CompilationException e) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.COMPILATION_FAILED, e.getMessage()));
        }
    }
}
