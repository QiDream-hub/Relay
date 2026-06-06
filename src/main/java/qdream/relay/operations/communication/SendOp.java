package qdream.relay.operations.communication;

import qdream.relay.engine.Executable;
import qdream.relay.types.NumberIota;
import qdream.relay.types.BooleanIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;

import com.google.gson.JsonObject;

import qdream.relay.core.CommunicationSystem;

/**
 * Send 操作 - 发送数据到频道
 * 弹出：频道号、数据
 */
public class SendOp implements Executable {
    private static final String ID = "relay:send";

    private static final int COST = 1;

    private static final OperationSignature SIGNATURE = OperationSignature.builder()
            .input("number")
            .input("any")
            .output("boolean")
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
        Executable dataData = executor.popData();
        if (dataData == null) return;
        Executable channelData = executor.popData();
        if (channelData == null) return;
        if (!(channelData instanceof NumberIota channel)) {
            executor.triggerMishap("操作 relay:send 期望 number 类型，实际为：" + channelData.getId());
            return;
        }

        int ch = channel.asInt();
        boolean success = CommunicationSystem.send(ch, dataData);

        if (!success) {
            executor.triggerMishap("操作 relay:send 频道 " + ch + " 队列已满");
            return;
        }

        executor.pushData(new BooleanIota(true));
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
            throw new IllegalArgumentException("Invalid ID for SendOp: " + id);
        }
        return new SendOp();
    }
}
