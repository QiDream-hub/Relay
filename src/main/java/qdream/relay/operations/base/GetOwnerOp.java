package qdream.relay.operations.base;

import net.minecraft.world.entity.Entity;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.EntityIota;

/**
 * GetOwner 操作 - 获取 Shell 的所有者（主人）
 *
 * 从上下文中获取 shellContainer 引用，然后获取其 owner 实体
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
                .output("relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 从上下文中获取 shellContainer
        ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);

        if (container == null) {
            executor.triggerMishap("无法获取容器：上下文缺失");
            return;
        }

        // 获取所有者实体
        Entity owner = container.getOwner();

        // 将所有者作为 EntityIota 压入数据栈
        // 如果没有主人，创建 null EntityIota
        executor.pushData(EntityIota.from(owner));
    }
}
