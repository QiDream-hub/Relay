package qdream.relay.operations.type;

import qdream.relay.types.TypeData;
import qdream.relay.types.StringData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * 将 TypeType 转换为 StringType 的操作
 * 将类型 ID 转换为字符串表示
 *
 * 弹出：type (relay:type)
 * 压入：string (relay:string) - 类型 ID 的字符串表示
 *
 * 示例用法：
 * 1. 获取类型字符串：get_type type_to_string
 * 2. 拼接类型名：get_type type_to_string "类型：" append
 * 3. 调试输出：get_type type_to_string print
 */
public class TypeToStringOp extends Spell {

    public TypeToStringOp() {
        super("relay:type_to_string", 1, 0.05, OperationSignature.builder()
                .consumesFromData("type", "relay:type")
                .producesToData("string", "relay:string")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出类型
        TypeData type = OperationHelpers.popType(executor, id);
        if (type == null) {
            return;
        }

        // 获取类型 ID 并转换为字符串
        String typeId = type.getId();
        executor.pushData(new StringData(typeId != null ? typeId : ""));
    }

}
