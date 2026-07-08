package qdream.relay.operations.type;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.StringData;

/**
 * 转换为字符数据操作
 * 调用任意 Executable 的 asString() 方法，将结果作为字符串压入数据栈
 *
 * 弹出：any (任意类型)
 * 压入：string (字符数据)
 *
 * 示例用法：
 * 1. 数字转字符串：123 to_string → "123"
 * 2. 布尔转字符串：true to_string → "true"
 * 3. 向量转字符串：{1,2,3} to_string → "{x:1.0,y:2.0,z:3.0}"
 * 4. 操作转字符串：relay:add to_string → "relay:add"
 */
public class ToStringOp extends Spell {

    public ToStringOp() {
        super("relay:to_string", 1, 1, OperationSignature.builder()
                .consumesFromData("any", "relay:any")
                .producesToData("string", "relay:string")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出任意类型
        Executable input = OperationHelpers.popAny(executor);
        if (input == null) {
            return;
        }

        if (input instanceof Operation op) {
            executor.pushData(new StringData(op.asString()));
        } else {
            executor.pushData(new StringData(input.toString()));
        }

    }
}
