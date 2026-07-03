package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.EntityType;

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
        Optional<ShellContainer> containerOpt = executor.getContext("shellContainer", ShellContainer.class);

        if (!containerOpt.isPresent()) {
            executor.triggerMishap("无法获取容器：上下文缺失");
            return;
        }

        ShellContainer container = containerOpt.get();
        Entity owner = container.getOwner();
        if (owner == null) {
            executor.triggerMishap("无法获取所属者");
            return;
        }
        executor.pushData(EntityType.from(owner, owner.level()));
    }
}
