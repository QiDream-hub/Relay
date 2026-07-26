package qdream.relay.operations.type;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.BlockData;
import qdream.relay.types.EntityData;
import qdream.relay.types.TypeData;

/**
 * 获取 Entity/BlockEntity/Block 的类型 ID 操作
 * 从实体、方块实体或方块类型中提取注册表 ID
 *
 * 弹出：entity/block_entity/block (输入类型)
 * 压入：type (Identifier 作为 TypeType)
 *
 * 支持的输入类型：
 * - relay:entity → 返回实体的 EntityType ID (例如 "minecraft:cow")
 * - relay:block_entity → 返回方块实体的 BlockEntityType ID (例如 "minecraft:furnace")
 * - relay:block → 返回方块的 Block ID (例如 "minecraft:stone")
 *
 * 示例用法：
 * 1. 获取实体类型 ID：get_entity get_type
 * 2. 获取方块实体类型 ID：some_vector get_block_entity get_type
 * 3. 获取方块 ID：some_vector get_block get_type
 * 4. 比较类型：get_entity get_type some_string eq if ...
 */
public class GetTypeOp extends Instruction {

    public GetTypeOp() {
        super("relay:get_type", 1, 1, OperationSignature.builder()
                .consumesFromData("input", "relay:entity", "relay:block_entity", "relay:block")
                .producesToData("type", "relay:type")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        Executable inputExe = StackHelpers.popAny(executor);
        if (inputExe == null)
            return;

        // 根据输入类型提取 Identifier
        if (inputExe instanceof EntityData entityType) {
            // 获取实体的 EntityType ID
            var entity = entityType.getEntity();
            if (entity == null) {
                executor.triggerMishap("实体引用为空");
                return;
            }
            // 使用 BuiltInRegistries 获取 EntityType 的注册表 ID
            var registryKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String id = registryKey != null ? registryKey.toString() : "unknown";
            executor.pushData(new TypeData(id));

        } else if (inputExe instanceof BlockEntityData blockEntityType) {
            // 获取方块实体的 BlockEntityType ID
            // 尝试从上下文获取世界
            Level level = getLevel(executor);
            var blockEntity = blockEntityType.getBlockEntity(level);
            if (blockEntity == null) {
                executor.triggerMishap("方块实体引用为空");
                return;
            }
            // 使用 BuiltInRegistries 获取 BlockEntityType 的注册表 ID
            var registryKey = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            String id = registryKey != null ? registryKey.toString() : "unknown";
            executor.pushData(new TypeData(id));

        } else if (inputExe instanceof BlockData blockType) {
            // 获取方块的 Block ID
            // 尝试从上下文获取世界
            Level level = getLevel(executor);
            var blockState = blockType.getBlockState(level);
            if (blockState == null) {
                executor.triggerMishap("方块状态引用为空");
                return;
            }
            // 使用 BuiltInRegistries 获取 Block 的注册表 ID
            var registryKey = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            String id = registryKey != null ? registryKey.toString() : "unknown";
            executor.pushData(new TypeData(id));

        } else {
            executor.triggerMishap("期望 entity/block_entity/block 类型");
            return;
        }
    }

    /**
     * 从上下文获取 Level
     */
    private Level getLevel(StateMachine executor) {
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        return levelOpt.orElse(null);
    }
}
