package qdream.relay.operations.control;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * If 操作 - 条件分支
 * 从数据栈弹出：条件、真分支列表、假分支列表
 * 根据条件将其中一个分支反转后压入程序栈
 */
public class IfOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota falseBranch = executor.popData();
        Iota trueBranch = executor.popData();
        Iota condition = executor.popData();
        
        if (falseBranch == null || trueBranch == null || condition == null) {
            return;
        }
        
        if (!condition.isBoolean()) {
            throw new IllegalArgumentException("If 需要一个布尔条件");
        }
        
        if (!trueBranch.isList()) {
            throw new IllegalArgumentException("If 的真分支必须是列表");
        }
        
        if (!falseBranch.isList()) {
            throw new IllegalArgumentException("If 的假分支必须是列表");
        }
        
        // 根据条件选择分支
        List<Iota> selected = condition.asBoolean() 
                ? trueBranch.asList() 
                : falseBranch.asList();
        
        // 反转后压入程序栈
        List<Iota> reversed = new ArrayList<>(selected);
        Collections.reverse(reversed);
        
        for (Iota iota : reversed) {
            executor.pushProgram(iota);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.BOOLEAN)
                .input(IotaType.LIST)
                .input(IotaType.LIST)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
