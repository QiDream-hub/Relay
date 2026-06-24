package qdream.relay.operations.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BlockEntityIota;
import qdream.relay.types.EntityIota;

/**
 * GetSelf 操作 - 获取自身外壳对应的实体/方块实体
 *
 * 从上下文中获取 shellContainer 和 world，然后根据容器类型返回对应的 Iota：
 * - 实体外壳：返回 EntityIota（实体）
 * - 方块外壳：返回 BlockEntityIota（方块实体）
 *
 * 弹出：无
 * 压入：entity 或 block_entity（自身对应的引用）
 *
 * 示例用法：
 * 1. 获取自身引用：get_self
 * 2. 检查是否是方块实体：get_self is_block_entity if { ... }
 * 3. 对实体进行操作：get_self some_entity_op
 */
public class GetSelfOp extends Spell {

    public GetSelfOp() {
        super("relay:get_self", 1, 1, OperationSignature.builder()
                .output("relay:entity")  // 可能是 EntityIota 或 BlockEntityIota
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 从上下文中获取 shellContainer 和 world
        ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);
        Level world = executor.getContext("world", Level.class);

        if (container == null) {
            executor.triggerMishap("无法获取自身容器：上下文缺失");
            return;
        }

        // 根据容器类型获取对应的引用
        if (container instanceof Entity entity) {
            // 实体外壳 - 返回 EntityIota
            executor.pushData(EntityIota.from(entity, world));
        } else if (container instanceof BlockEntity blockEntity) {
            // 方块外壳 - 返回 BlockEntityIota
            executor.pushData(BlockEntityIota.from(blockEntity, world));
        }
        // 工具外壳在命令执行时处理，这里不会遇到
    }
}
