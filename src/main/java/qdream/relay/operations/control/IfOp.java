package qdream.relay.operations.control;

import qdream.relay.engine.IExecutable;
import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

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
        IData falseBranchData = executor.popData();
        if (!(falseBranchData instanceof McIota falseBranch)) return;
        IData trueBranchData = executor.popData();
        if (!(trueBranchData instanceof McIota trueBranch)) return;
        IData conditionData = executor.popData();
        if (!(conditionData instanceof McIota condition)) return;
        
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
        List<IExecutable> selected = condition.asBoolean() 
                ? trueBranch.asList() 
                : falseBranch.asList();
        
        // 反转后压入程序栈
        List<IExecutable> reversed = new ArrayList<>(selected);
        Collections.reverse(reversed);

        for (IExecutable iota : reversed) {
            executor.pushProgram(iota);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("boolean")
                .input("list")
                .input("list")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
