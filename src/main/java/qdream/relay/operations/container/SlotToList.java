package qdream.relay.operations.container;

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
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ITEM_NOT_FOUND));
        }

        // 检查物品是否是法术磁盘（检查是否是 DiskComponent 类型）
        if (!(itemStack.getItem() instanceof DiskComponent diskComponent)) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.NOT_A_SPELL_DISK));
        }

        // 从磁盘读取程序列表
        String programJson = diskComponent.getProgram(itemStack);
        java.util.List<Executable> program;
        try {
            program = ProgramCompiler.compileFromJson(programJson);
        } catch (ProgramCompiler.CompilationException e) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.COMPILATION_FAILED, e.getMessage()));
        }

        // 将程序列表包装为 ListData 并压入数据栈
        executor.pushData(new ListData(new ArrayList<>(program)));
    }
}
