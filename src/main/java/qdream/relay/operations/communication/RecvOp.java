package qdream.relay.operations.communication;

import qdream.relay.engine.Executable;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;

import com.google.gson.JsonObject;

import qdream.relay.core.CommunicationSystem;

/**
 * Recv 操作 - 接收数据（出队）
 * 弹出：频道号
 * 返回：数据或 null
 */
public class RecvOp implements Executable {
    private static final String ID = "relay:recv";

    private static final int COST = 1;

    private static final OperationSignature SIGNATURE = OperationSignature.builder()
            .input("number")
            .output("any")
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
        Executable channelData = executor.popData();
        if (channelData == null) return;
        if (!(channelData instanceof NumberIota channel)) {
            executor.triggerMishap("操作 relay:recv 期望 number 类型，实际为：" + channelData.getId());
            return;
        }

        int ch = channel.asInt();
        Executable data = CommunicationSystem.recv(ch);
        executor.pushData(data);
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
            throw new IllegalArgumentException("Invalid ID for RecvOp: " + id);
        }
        return new RecvOp();
    }
}
