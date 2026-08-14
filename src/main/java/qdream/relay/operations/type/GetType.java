package qdream.relay.operations.type;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.TypeException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.TextTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.BlockData;
import qdream.relay.types.EntityData;
import qdream.relay.types.SlotData;
import qdream.relay.types.TypeData;

/**
 * 获取类型 ID 操作
 * 从实体、方块实体、方块或物品插槽中提取注册表 ID
 *
 * 弹出：input (实体/方块实体/方块/物品插槽)
 * 压入：type (Identifier 作为 TypeType)
 *
 * 支持的输入类型：
 * - relay:entity → 返回实体的 EntityType ID (例如 "minecraft:cow")
 * - relay:block_entity → 返回方块实体的 BlockEntityType ID (例如 "minecraft:furnace")
 * - relay:block → 返回方块的 Block ID (例如 "minecraft:stone")
 * - relay:slot → 返回插槽内物品的 Item ID (例如 "minecraft:diamond")
 *
 * 示例用法：
 * 1. 获取实体类型 ID：get_self get_type
 * 2. 获取方块实体类型 ID：some_vector get_block_entity get_type
 * 3. 获取方块 ID：some_vector get_block get_type
 * 4. 获取物品类型 ID：item_slot get_type
 * 5. 比较类型：get_entity get_type some_string eq if ...
 */
public class GetType extends Instruction {

    public GetType() {
        super("relay:get_type", 1, 0.1, OperationSignature.builder()
                .consumesFromData("input", "relay:entity", "relay:block_entity", "relay:block", "relay:slot")
                .producesToData("type", "relay:type")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        Executable inputExe = StackHelpers.popAny(executor, id);

        // 根据输入类型提取 Identifier
        if (inputExe instanceof EntityData entityType) {
            // 获取实体的 EntityType ID
            var entity = entityType.getEntity();
            if (entity == null) {
                throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY_REFERENCE));
            }
            // 使用 BuiltInRegistries 获取 EntityType 的注册表 ID
            var registryKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String id = registryKey != null ? registryKey.toString() : "unknown";
            executor.pushData(new TypeData(id));

        } else if (inputExe instanceof BlockEntityData blockEntityType) {
            // 获取方块实体的 BlockEntityType ID
            var blockEntity = blockEntityType.getBlockEntity();
            if (blockEntity == null) {
                throw new TypeException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.BLOCK_ENTITY_REFERENCE_INVALID));
            }
            // 使用 BuiltInRegistries 获取 BlockEntityType 的注册表 ID
            var registryKey = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            String id = registryKey != null ? registryKey.toString() : "unknown";
            executor.pushData(new TypeData(id));

        } else if (inputExe instanceof BlockData blockType) {
            // 获取方块的 Block ID
            var blockState = blockType.getBlockState();
            if (blockState == null) {
                throw new TypeException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.BLOCK_REFERENCE_INVALID));
            }
            // 使用 BuiltInRegistries 获取 Block 的注册表 ID
            var registryKey = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            String id = registryKey != null ? registryKey.toString() : "unknown";
            executor.pushData(new TypeData(id));

        } else if (inputExe instanceof SlotData slotData) {
            // 获取插槽内物品的 Item ID
            OperationHelpers.checkWorldInteractor(executor, id);
            
            ItemStack itemStack = slotData.getItemStack();
            if (itemStack == null || itemStack.isEmpty()) {
                throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ITEM_NOT_FOUND));
            }
            
            // 从物品注册表 ID 获取类型 ID
            String typeId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
            executor.pushData(new TypeData(typeId));

        } else {
            throw new TypeException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.PARAMETER_INVALID, TextTools.getName(inputExe)));
        }
    }
}
