package qdream.relay.operations.type;

import qdream.relay.types.TypeData;
import qdream.relay.types.BooleanData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * 比较两个 TypeType 是否相等的操作
 * 
 * 弹出：type_a (relay:type), type_b (relay:type)
 * 压入：result (relay:boolean) - 如果两个类型 ID 相同则为 true
 * 
 * 示例用法：
 * 1. 比较两个类型：type_a type_b type_eq
 * 2. 条件判断：type_a type_b type_eq if ...
 */
public class TypeEqualsOp extends Spell {

    public TypeEqualsOp() {
        super("relay:type_eq", 1, 1, OperationSignature.builder()
                .consumesFromData("type_a", "relay:type")
                .consumesFromData("type_b", "relay:type")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出两个类型（注意顺序：先弹出的是 type_b）
        TypeData typeB = (TypeData) executor.popData();
        if (typeB == null) {
            executor.triggerMishap("数据栈不足，需要第二个 type");
            return;
        }
        
        TypeData typeA = (TypeData) executor.popData();
        if (typeA == null) {
            executor.triggerMishap("数据栈不足，需要第一个 type");
            return;
        }

        // 使用 TypeType.equalsTo() 方法比较
        boolean result = typeA.equalsTo(typeB);
        executor.pushData(new BooleanData(result));
    }

}
