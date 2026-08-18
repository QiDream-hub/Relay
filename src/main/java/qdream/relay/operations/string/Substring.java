package qdream.relay.operations.string;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.StringData;

/**
 * 字符串截取操作
 *
 * 弹出：
 * - end (结束索引，不包含)
 * - start (起始索引，包含)
 * - string (原字符串)
 *
 * 压入：
 * - string (截取后的子字符串)
 *
 * 示例用法：
 * 1. 截取部分字符串："Hello World" 0 5 substring → "Hello"
 * 2. 获取后缀："file.txt" 5 -1 substring → ".txt" (-1 表示到末尾)
 */
public class Substring extends Instruction {

    public Substring() {
        super("relay:substring", 1, 0.1, OperationSignature.builder()
                .consumesFromData("string", "relay:string")
                .consumesFromData("start", "relay:number")
                .consumesFromData("end", "relay:number")
                .producesToData("result", "relay:string")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 按签名声明顺序弹出：先 string，后 start，最后 end
        // 由于栈是后进先出，实际弹出顺序与声明相反
        StringData string = StackHelpers.popString(executor, id);
        NumberData start = StackHelpers.popNumber(executor, id);
        NumberData end = StackHelpers.popNumber(executor, id);

        String str = string.getValue().getString();
        int startIndex = (int) start.asDouble();
        int endIndex = end.isInteger() && end.asInt() < 0 
            ? str.length() 
            : (int) end.asDouble();

        // 边界检查
        startIndex = Math.max(0, Math.min(startIndex, str.length()));
        endIndex = Math.max(0, Math.min(endIndex, str.length()));

        if (startIndex > endIndex) {
            int temp = startIndex;
            startIndex = endIndex;
            endIndex = temp;
        }

        // 截取子字符串
        String substring = str.substring(startIndex, endIndex);
        executor.pushData(new StringData(Component.literal(substring)));
    }
}
