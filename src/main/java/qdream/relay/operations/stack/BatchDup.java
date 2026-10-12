package qdream.relay.operations.stack;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.stack.BatchCountExceedsStackError;
import qdream.relay.mc.errors.parameter.BatchCountMustBeIntegerError;
import qdream.relay.mc.errors.parameter.BatchCountMustBeNumberError;
import qdream.relay.mc.errors.parameter.BatchCountMustBePositiveError;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量复制操作 - 复制数据栈顶部 N 个元素
 */
public class BatchDup extends Instruction {

    public BatchDup() {
        super("relay:batch_dup", 1, 0.05, OperationSignature.builder()
                .consumesFromData("count", "relay:number")
                .consumesFromData("many", "...any")
                .producesToData("copiesList", "...any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable countExe = StackHelpers.popAny(executor, id);

        if (!(countExe instanceof NumberData numberData)) {
            throw new BatchCountMustBeNumberError(executor);
        }

        if (!numberData.isInteger()) {
            throw new BatchCountMustBeIntegerError(executor);
        }

        int count = numberData.asInt();
        if (count <= 0) {
            throw new BatchCountMustBePositiveError(executor);
        }

        if (count > executor.getDataStackSize()) {
            throw new BatchCountExceedsStackError(executor);
        }

        // 获取栈顶 N 个元素（从栈顶到栈底）
        List<Executable> topElements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            topElements.add(executor.popData());
        }

        // 恢复原元素
        for (int i = count - 1; i >= 0; i--) {
            executor.pushData(topElements.get(i));
        }

        // 再次压入这些元素（实现复制）
        for (int i = count - 1; i >= 0; i--) {
            executor.pushData(topElements.get(i));
        }
    }
}
