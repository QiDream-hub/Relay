package qdream.relay.operations.entity;

import net.minecraft.world.entity.player.Player;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;

/**
 * GetOwner 操作 - 获取 Shell 的所有者（主人）
 *
 * 从上下文中获取 shellContainer 和 world，然后获取其 owner 实体
 *
 * 弹出：无
 * 压入：entity (所有者实体)
 *
 * 外壳没有主人时触发 {@link qdream.relay.mc.errors.entity.OwnerNotFoundError} 事故并终止程序。
 *
 * 示例用法：
 * 1. 获取主人：get_owner
 * 2. 对主人进行操作：get_owner some_entity_op
 */
public class GetOwner extends Instruction {

    public GetOwner() {
        super("relay:get_owner", 1, 1, OperationSignature.builder()
                .producesToData("owner", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {

        Player owner = OperationHelpers.getOwner(executor);

        executor.pushData(EntityData.from(owner, owner.level()));
    }
}
