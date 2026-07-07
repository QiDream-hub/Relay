package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;

/**
 * GetOwner 操作 - 获取 Shell 的所有者（主人）
 *
 * 从上下文中获取 shellContainer 和 world，然后获取其 owner 实体
 *
 * 弹出：无
 * 压入：entity (所有者实体，如果没有主人则压入 null entity)
 *
 * 示例用法：
 * 1. 获取主人：get_owner
 * 2. 检查是否有主人：get_owner is_null if ...
 * 3. 对主人进行操作：get_owner some_entity_op
 */
public class GetOwnerOp extends Spell {

    public GetOwnerOp() {
        super("relay:get_owner", 1, 1, OperationSignature.builder()
                .producesToData("owner", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {

        ShellContainer shellContainer = OperationHelpers.getShellContainer(executor);

        Entity owner = shellContainer.getOwner();
        if (owner == null) {
            executor.triggerMishap("无法获取所属者");
            return;
        }

        executor.pushData(EntityData.from(owner, owner.level()));
    }
}
