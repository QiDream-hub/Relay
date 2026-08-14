package qdream.relay.operations.logic;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;

/**
 * 转换为逻辑类型操作
 * 调用任意 Executable 的 asBoolean() 方法，将结果作为布尔值压入数据栈
 *
 * 弹出：any (任意类型)
 * 压入：boolean (逻辑数据)
 *
 * 转换规则：
 * - 布尔类型：返回自身值
 * - 数字类型：非零为 true，零为 false
 * - 向量类型：任意分量非零为 true，全零为 false
 * - 字符串类型：非空为 true，空为 false
 * - 列表类型：非空为 true，空为 false
 * - 实体/方块/方块实体类型：有效引用为 true，null 为 false
 * - 空值类型：始终为 false
 * - 类型类型：非空 ID 为 true，空为 false
 *
 * 示例用法：
 * 1. 数字转布尔：0 to_bool → false, 1 to_bool → true
 * 2. 字符串转布尔："" to_bool → false, "hello" to_bool → true
 * 3. 列表转布尔：[] to_bool → false, [1] to_bool → true
 * 4. 布尔自身：true to_bool → true, false to_bool → false
 */
public class ToBoolean extends Instruction {

    public ToBoolean() {
        super("relay:to_bool", 1, 0.05, OperationSignature.builder()
                .consumesFromData("inputValue", "any")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出任意类型
        Executable input = StackHelpers.popAny(executor, id);

        // 调用 asBoolean() 方法
        boolean result;
        if (input instanceof Operation op) {
            result = op.asBoolean();
        } else {
            // 非 Operation 类型返回 false
            result = false;
        }
        executor.pushData(new BooleanData(result));
    }
}
