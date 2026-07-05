package qdream.relay.operations.base;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberData;

import java.util.ArrayList;
import java.util.List;

/**
 * 移到栈顶操作 - 将指定位置的值移到栈顶
 * 位置从 0 开始计数，0 表示栈顶，1 表示栈顶下一个元素
 */
public class MoveToTopOp extends Spell {

    public MoveToTopOp() {
        super("relay:move_to_top", 1, 1, OperationSignature.builder()
                .consumesFromData("index", "number")
                .producesToData("value", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable indexExe = executor.popData();
        if (indexExe == null) {
            return;
        }

        if (!(indexExe instanceof NumberData numberData)) {
            executor.triggerMishap("移到栈顶：参数必须是数字");
            return;
        }

        if (!numberData.isInteger()) {
            executor.triggerMishap("移到栈顶：索引必须是整数");
            return;
        }

        int index = numberData.asInt();
        if (index < 0) {
            executor.triggerMishap("移到栈顶：索引不能为负数");
            return;
        }

        if (index >= executor.getDataStackSize()) {
            executor.triggerMishap("移到栈顶：索引超出栈范围");
            return;
        }

        // 获取当前栈
        List<Executable> stack = executor.getDataStackSnapshot();
        
        // 计算实际位置（栈顶为 0，向下递增）
        // Deque 的 push/pop 都是操作头部，所以位置 0 就是索引 0
        int targetPos = index;
        if (targetPos < 0 || targetPos >= stack.size()) {
            executor.triggerMishap("移到栈顶：位置无效");
            return;
        }

        // 弹出栈顶到目标位置之前的所有元素
        List<Executable> above = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            above.add(executor.popData());
        }

        // 弹出目标元素
        Executable target = executor.popData();
        if (target == null) {
            executor.triggerMishap("移到栈顶：无法获取目标元素");
            return;
        }

        // 先将目标元素压入栈顶
        executor.pushData(target);

        // 再压回上面的元素（需要反转）
        for (int i = above.size() - 1; i >= 0; i--) {
            executor.pushData(above.get(i));
        }
    }
}
