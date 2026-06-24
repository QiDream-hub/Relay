package qdream.relay.operations.communication;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.EntityIota;
import qdream.relay.types.StringIota;
import qdream.relay.types.BooleanIota;

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
                .input("relay:entity")
                .input("relay:string")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出实体
        var entityExe = executor.popData();
        if (entityExe == null) {
            executor.triggerMishap("无法弹出实体");
            return;
        }
        
        if (!(entityExe instanceof EntityIota entityIota)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        // 弹出消息字符串
        var msgExe = executor.popData();
        if (msgExe == null) {
            executor.triggerMishap("无法弹出消息");
            return;
        }
        
        if (!(msgExe instanceof StringIota stringIota)) {
            executor.triggerMishap("期望 string 类型");
            return;
        }

        // 获取实体引用
        Entity entity = entityIota.getEntity();
        String message = stringIota.asString();

        // 检查是否是玩家
        if (entity instanceof Player player) {
            player.sendSystemMessage(Component.literal(message));
            executor.pushData(new BooleanIota(true));
        } else {
            // 不是玩家或实体不存在，返回 false
            executor.pushData(new BooleanIota(false));
        }
    }
}
