package qdream.relay.operations.string;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.StringData;

/**
 * 字符串比较操作
 *
 * 弹出：
 * - string2 (第二个字符串)
 * - string1 (第一个字符串)
 *
 * 压入：
 * - boolean (string1 是否等于 string2)
 *
 * 示例用法：
 * 1. 比较字符串："Hello" "Hello" string_equals → true
 * 2. 检查空字符串：input "" string_equals
 */
public class StringEquals extends Instruction {

    public StringEquals() {
        super("relay:string_equals", 1, 0.05, OperationSignature.builder()
                .consumesFromData("string1", "relay:string")
                .consumesFromData("string2", "relay:string")
                .producesToData("equal", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 按签名声明顺序弹出：先 string1，后 string2
        // 由于栈是后进先出，实际弹出顺序与声明相反
        StringData string1 = StackHelpers.popString(executor, id);
        StringData string2 = StackHelpers.popString(executor, id);

        // 比较字符串内容
        boolean equals = string1.getValue().getString().equals(string2.getValue().getString());
        executor.pushData(new BooleanData(equals));
    }
}
