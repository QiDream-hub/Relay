package qdream.relay.operations.communication;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.StringData;
import qdream.relay.types.BooleanData;

/**
 * SendMessageOp 操作 - 向玩家实体发送聊天消息
 *
 * 弹出：
 * - entity (接收消息的实体，必须是玩家)
 * - string (消息内容)
 *
 * 压入：
 * - boolean (是否成功发送)
 *
 * 示例用法：
 * 1. 向主人发送消息：get_owner "Hello!" send_message
 * 2. 检查发送结果：get_owner "Hello!" send_message if { ... }
 */
public class SendMessageOp extends Spell {

    public SendMessageOp() {
        super("relay:send_message", 2, 1, OperationSignature.builder()
                .consumesFromData("recipient", "relay:entity")
                .consumesFromData("message", "relay:string")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        // 先弹出消息（后压入的先弹出）
        StringData message = OperationHelpers.popString(executor, id);
        if (message == null) return;

        // 再弹出实体
        EntityData recipient = OperationHelpers.popEntity(executor, id);
        if (recipient == null) return;

        // 获取实体引用
        Entity entity = recipient.getEntity();
        String msg = message.asString();

        // 检查是否是玩家
        if (entity instanceof Player player) {
            player.sendSystemMessage(Component.literal(msg));
            executor.pushData(new BooleanData(true));
        } else {
            // 不是玩家或实体不存在，返回 false
            executor.pushData(new BooleanData(false));
        }
    }
}
