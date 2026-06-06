package qdream.relay.operations.list;

import qdream.relay.types.ProgramBlock;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

import java.util.List;

/**
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp implements Executable {
    private static final String ID = "relay:list_length";

    private static final int COST = 1;

    private static final OperationSignature SIGNATURE = OperationSignature.builder()
            .input("list")
            .output("number")
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
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_length 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        executor.pushData(new NumberIota(list.size()));
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
            throw new IllegalArgumentException("Invalid ID for ListLengthOp: " + id);
        }
        return new ListLengthOp();
    }
}
