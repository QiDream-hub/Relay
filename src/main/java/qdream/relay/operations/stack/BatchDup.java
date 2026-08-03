package qdream.relay.operations.stack;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
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
                .producesToData("copies", "...any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable countExe = executor.popData();
        if (countExe == null) {
            return;
        }

        if (!(countExe instanceof NumberData numberData)) {
            executor.triggerMishap("批量复制：参数必须是数字");
            return;
        }

        if (!numberData.isInteger()) {
            executor.triggerMishap("批量复制：计数必须是整数");
            return;
        }

        int count = numberData.asInt();
        if (count <= 0) {
            executor.triggerMishap("批量复制：计数必须大于 0");
            return;
        }

        if (count > executor.getDataStackSize()) {
            executor.triggerMishap("批量复制：计数超出栈大小");
            return;
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
