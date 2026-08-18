package qdream.relay.operations.string;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.StringData;
import qdream.relay.types.BooleanData;

/**
 * SendMessageOp 操作 - 向玩家实体发送聊天消息
 *
 * 弹出：
 * - message (消息内容)
 * - recipient (接收消息的实体，必须是玩家)
 *
 * 压入：
 * - boolean (是否成功发送)
 *
 * 示例用法：
 * 1. 向主人发送消息：get_owner "Hello!" send_message
 * 2. 检查发送结果：get_owner "Hello!" send_message if { ... }
 */
public class SendMessage extends Instruction {

    public SendMessage() {
        super("relay:send_message", 1, 1.5, OperationSignature.builder()
                .consumesFromData("recipient", "relay:entity")
                .consumesFromData("message", "relay:string")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        OperationHelpers.checkWorldInteractor(executor, id);

        // 按签名声明顺序弹出：先 recipient，后 message
        // 由于栈是后进先出，实际弹出顺序与声明相反
        EntityData recipient = StackHelpers.popEntity(executor, id);
        StringData message = StackHelpers.popString(executor, id);

        // 获取实体引用
        Entity entity = recipient.getEntity();
        Component msg = message.getValue();

        // 检查是否是玩家
        if (entity instanceof Player player) {
            player.sendSystemMessage(msg);
            executor.pushData(new BooleanData(true));
        } else {
            // 不是玩家或实体不存在，返回 false
            executor.pushData(new BooleanData(false));
        }
    }
}
