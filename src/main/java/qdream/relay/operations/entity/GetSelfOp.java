package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BlockEntityType;
import qdream.relay.types.EntityType;
import qdream.relay.types.NullType;

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
                .producesToData("self", "relay:entity", "relay:block_entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 尝试从上下文中获取 self 实体（Entity 或 BlockEntity）
        Optional<Object> selfObj = executor.getContext("self", Object.class);

        if (!selfObj.isPresent() || selfObj.get() == null) {
            executor.pushData(NullType.INSTANCE);
            return;
        }

        Object self = selfObj.get();

        // 根据类型创建对应的 Iota
        if (self instanceof Entity entity) {
            EntityType entityType = EntityType.from(entity, entity.level());
            executor.pushData(entityType);
        } else if (self instanceof BlockEntity blockEntity) {
            BlockEntityType blockEntityType = BlockEntityType.from(blockEntity, blockEntity.getLevel());
            executor.pushData(blockEntityType);
        } else {
            executor.pushData(NullType.INSTANCE);
        }
    }
}
