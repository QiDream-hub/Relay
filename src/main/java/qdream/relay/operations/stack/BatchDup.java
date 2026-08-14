package qdream.relay.operations.stack;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.errors.StackException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
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
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.BATCH_COUNT_MUST_BE_NUMBER));
        }

        if (!numberData.isInteger()) {
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.BATCH_COUNT_MUST_BE_INTEGER));
        }

        int count = numberData.asInt();
        if (count <= 0) {
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.BATCH_COUNT_MUST_BE_POSITIVE));
        }

        if (count > executor.getDataStackSize()) {
            throw new StackException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.BATCH_COUNT_EXCEEDS_STACK));
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
