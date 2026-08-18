package qdream.relay.operations.string;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.StringData;

/**
 * 字符串长度操作
 *
 * 弹出：
 * - string (输入字符串)
 *
 * 压入：
 * - number (字符串长度)
 *
 * 示例用法：
 * 1. 获取字符串长度："Hello" length → 5
 * 2. 检查空字符串："" length 0 eq → true
 */
public class StringLength extends Instruction {

    public StringLength() {
        super("relay:string_length", 1, 0.05, OperationSignature.builder()
                .consumesFromData("string", "relay:string")
                .producesToData("length", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出字符串
        StringData str = StackHelpers.popString(executor, id);

        // 计算长度
        int length = str.getValue().getString().length();
        executor.pushData(new NumberData(length));
    }
}
