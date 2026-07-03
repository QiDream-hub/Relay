package qdream.relay.operations.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanType;
import qdream.relay.types.EntityType;

/**
 * IsPlayerOp 操作 - 检测实体是否是玩家
 *
 * 弹出：
 * - entity (要检测的实体)
 *
 * 压入：
 * - boolean (是否是玩家)
 *
 * 示例用法：
 * 1. 检测主人是否是玩家：get_owner is_player if { ... }
 * 2. 检测实体类型：some_entity is_player
 */
public class IsPlayerOp extends Spell {

    public IsPlayerOp() {
        super("relay:is_player", 1, 1, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("isPlayer", "relay:boolean")
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

        if (!(entityExe instanceof EntityType entityIota)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        // 获取实体引用（需要世界）
        Entity entity = entityIota.getEntity();

        // 检查是否是玩家
        boolean isPlayer = (entity instanceof Player);
        executor.pushData(new BooleanType(isPlayer));
    }
}
