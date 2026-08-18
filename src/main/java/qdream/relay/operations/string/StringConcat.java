package qdream.relay.operations.string;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.StringData;

/**
 * 字符串拼接操作
 *
 * 弹出：
 * - string2 (第二个字符串)
 * - string1 (第一个字符串)
 *
 * 压入：
 * - string (拼接后的结果 string1 + string2)
 *
 * 示例用法：
 * 1. 拼接两个字符串："Hello" "World" string_concat → "HelloWorld"
 * 2. 拼接数字和字符串：123 to_string " apples" string_concat → "123 apples"
 */
public class StringConcat extends Instruction {

    public StringConcat() {
        super("relay:string_concat", 1, 0.1, OperationSignature.builder()
                .consumesFromData("string1", "relay:string")
                .consumesFromData("string2", "relay:string")
                .producesToData("result", "relay:string")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 按签名声明顺序弹出：先 string1，后 string2
        // 由于栈是后进先出，实际弹出顺序与声明相反
        StringData string1 = StackHelpers.popString(executor, id);
        StringData string2 = StackHelpers.popString(executor, id);

        // 拼接字符串 (string1 + string2)
        String combined = string1.getValue().getString() + string2.getValue().getString();
        executor.pushData(new StringData(Component.literal(combined)));
    }
}
