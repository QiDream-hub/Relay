package qdream.relay.operations.control;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.Executable;
import qdream.relay.types.ProgramBlock;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * If 操作 - 条件分支
 * 从数据栈弹出：条件、真分支列表、假分支列表
 * 根据条件将其中一个分支反转后压入程序栈
 */
public class IfOp extends AbstractOperation {

    protected IfOp() {
        super("relay:if", 1, OperationSignature.builder()
                .input("boolean")
                .input("list")
                .input("list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable falseBranchData = executor.popData();
        if (falseBranchData == null)
            return;
        if (!(falseBranchData instanceof ProgramBlock falseBranch)) {
            executor.triggerMishap("操作 relay:if 期望 list 类型，实际为：" + falseBranchData.getId());
            return;
        }
        Executable trueBranchData = executor.popData();
        if (trueBranchData == null)
            return;
        if (!(trueBranchData instanceof ProgramBlock trueBranch)) {
            executor.triggerMishap("操作 relay:if 期望 list 类型，实际为：" + trueBranchData.getId());
            return;
        }
        Executable conditionData = executor.popData();
        if (conditionData == null)
            return;
        if (!(conditionData instanceof BooleanIota condition)) {
            executor.triggerMishap("操作 relay:if 期望 boolean 类型，实际为：" + conditionData.getId());
            return;
        }

        // 根据条件选择分支
        List<Executable> selected = condition.asBoolean()
                ? trueBranch.getItems()
                : falseBranch.getItems();

        // 反转后压入程序栈
        List<Executable> reversed = new ArrayList<>(selected);
        Collections.reverse(reversed);

        for (Executable iota : reversed) {
            executor.pushProgram(iota);
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        return json;
    }

    @Override
    public Executable fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for IfOp: " + id);
        }
        return new IfOp();
    }
}
