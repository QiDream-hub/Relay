package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.base.OperationHelpers;
import qdream.relay.types.ListData;

import java.util.ArrayList;

/**
 * ForOp - 列表迭代器
 *
 * <p>
 * 栈式迭代器设计：每次执行从列表取出第一个元素，剩余元素重新压入数据栈，
 * 实现无状态循环。程序结构：<code>[list, body, ForOp]</code> 会自动展开为
 * 多次迭代。
 * </p>
 *
 * <h3>执行流程</h3>
 * <ol>
 * <li>弹出列表，取出首元素</li>
 * <li>剩余列表自动重新压入数据栈</li>
 * <li>执行 body，body 完成后再次遇到 ForOp</li>
 * <li>重复直到列表为空</li>
 * </ol>
 *
 * <h3>示例</h3>
 *
 * <pre>
 * 程序：[List[1,2,3], PrintOp, ForOp]
 * 第 1 轮：弹出 1，剩余 [2,3] 压回 → 执行 PrintOp(1)
 * 第 2 轮：弹出 2，剩余 [3] 压回 → 执行 PrintOp(2)
 * 第 3 轮：弹出 3，无剩余 → 执行 PrintOp(3)
 * 第 4 轮：空列表 → 遍历结束
 * </pre>
 */
public class ForOp extends Spell {

    public ForOp() {
        super("relay:for", 1, 1, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .consumesFromProgram("body", "any", "relay:list")
                .producesToData("element", "any")
                .producesToProgram("body", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ListData list = OperationHelpers.popList(executor, "relay:for");
        if (list == null) return;
        
        Executable body = executor.peekProgram();
        if (body == null) return;

        var elements = list.getValue();
        if (elements.isEmpty()) {
            // 移除空的列表，终止循环
            executor.popProgram();
            return;
        }

        executor.pushProgram(this);

        // 取出第一个元素
        Executable first = elements.removeFirst();
        // 将修改后的 list 放回数据栈中
        executor.pushData(new ListData(elements));

        // 压入当前元素到数据栈
        executor.pushData(first);

        // 处理执行体：如果是程序列表则加载，否则压入单个操作
        if (body instanceof ListData programList) {
            executor.loadProgram(programList.getValue());
        } else {
            executor.pushProgram(body);
        }
    }
}
