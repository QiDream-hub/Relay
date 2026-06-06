package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.types.ProgramBlock;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.ArrayList;

/**
 * List Append 操作 - 在列表末尾添加元素
 * 输入：列表，值
 * 输出：新列表
 */
public class ListAppendOp implements Executable {
    private static final String ID = "relay:list_append";

    private static final int COST = 2;

    private static final OperationSignature SIGNATURE = OperationSignature.builder()
            .input("list")
            .input("any")
            .output("list")
            .build();

    public String getId() {
        return ID;
    }

    public int getCost() {
        return COST;
    }

    public OperationSignature getSignature() {
        return SIGNATURE;
    }

    @Override
    public void execute(StateMachine executor) {
        Executable valueData = executor.popData();
        if (valueData == null) return;
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_append 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        // 创建新列表（不可变修改）
        List<Executable> newList = new ArrayList<>(list);
        // 所有 IData 现在都实现 IExecutable
        if (valueData instanceof Executable exec) {
            newList.add(exec);
        }
        executor.pushData(new ProgramBlock(newList));
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
        if (!ID.equals(id)) {
            throw new IllegalArgumentException("Invalid ID for ListAppendOp: " + id);
        }
        return new ListAppendOp();
    }
}
