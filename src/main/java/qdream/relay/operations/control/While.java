package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.ListData;

/**
 * WhileOp - 条件循环迭代器
 *
 * <p>
 * 栈式迭代器设计：每次执行从数据栈弹出一个布尔值，如果为 true 则将执行体压入程序栈，
 * 同时将自身重新压入程序栈以实现下一轮检查。程序结构：<code>[condition, body, WhileOp]</code>
 * 会在条件为 true 时循环执行。
 * </p>
 *
 * <h3>执行流程</h3>
 * <ol>
 * <li>弹出布尔值</li>
 * <li>如果为 true：压入 body 和 WhileOp，继续下一轮</li>
 * <li>如果为 false：循环结束</li>
 * </ol>
 *
 * <h3>示例</h3>
 *
 * <pre>
 * 程序：[Boolean(true), DecrementOp, WhileOp]
 * 第 1 轮：条件为 true → 执行 DecrementOp
 * 第 2 轮：条件为 true → 执行 DecrementOp
 * 第 3 轮：条件为 false → 循环结束
 * </pre>
 *
 * <h3>注意事项</h3>
 * <p>
 * 条件值必须在每次 body 执行后被重新计算并压入数据栈，否则循环将无限执行或立即结束。
 * </p>
 */
public class While extends Instruction {

    public While() {
        super("relay:while", 1, 0.25, OperationSignature.builder()
                .consumesFromData("condition", "relay:boolean")
                .consumesFromProgram("body", "any", "relay:list")
                .producesToProgram("body", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData condition = StackHelpers.popBoolean(executor, id);

        Executable body = executor.peekProgram();

        // 如果条件为 true，执行 body 并继续循环
        if (condition.asBoolean()) {
            // 重新压入 WhileOp 以继续下一轮检查
            executor.pushProgram(this);
            // 压入 body 执行
            if (body instanceof ListData programList) {
                executor.loadProgram(programList.getValue());
            } else {
                executor.pushProgram(body);
            }
        } else {
            executor.popProgram();
        }
    }
}
