package qdream.relay.operations.item;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;
import qdream.relay.types.ItemData;

import java.util.Optional;

/**
 * 拾取物品操作
 * 将物品实体放入 BlockEntity 容器中
 *
 * <h3>执行流程</h3>
 * <ol>
 * <li>弹出 BlockEntityData 和 EntityData</li>
 * <li>验证实体是物品实体</li>
 * <li>通过 BlockEntityData 获取容器</li>
 * <li>尝试将物品放入容器（查找第一个可放入的空格或可堆叠槽）</li>
 * <li>放入成功：移除实体，返回 ItemData</li>
 * <li>放入失败：返回 false</li>
 * </ol>
 *
 * <h3>签名</h3>
 * 
 * <pre>
 * 弹出：container (方块实体), entity (物品实体)
 * 压入：success (布尔值 false 表示失败) 或 item (物品数据表示成功)
 * </pre>
 *
 * <h3>需求</h3>
 * <ul>
 * <li>需要世界交互器</li>
 * <li>需要检查范围</li>
 * </ul>
 */
public class PickupItemOp extends Spell {

    public PickupItemOp() {
        super("relay:pickup_item", 1, 2, OperationSignature.builder()
                .consumesFromData("container", "relay:block_entity")
                .consumesFromData("entity", "relay:entity")
                .producesToData("result", "relay:boolean", "relay:item")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出 BlockEntityData
        BlockEntityData blockEntityData = OperationHelpers.popBlockEntity(executor, id);
        if (blockEntityData == null || blockEntityData.isNull()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出 EntityData
        EntityData entityData = OperationHelpers.popEntity(executor, id);
        if (entityData == null || entityData.isNull()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取世界
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }
        Level world = levelOpt.get();

        if (world.isClientSide()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 BlockEntity
        var blockEntity = blockEntityData.getBlockEntity(world);
        if (blockEntity == null || blockEntity.isRemoved()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 检查是否为 Container
        if (!(blockEntity instanceof Container container)) {
            executor.triggerMishap("方块实体不是容器");
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取实体
        Entity entity = entityData.getEntity();
        if (entity == null || entity.isRemoved()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 检查是否为物品实体
        if (!(entity instanceof ItemEntity itemEntity)) {
            executor.triggerMishap("实体不是物品实体");
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取物品堆
        ItemStack itemStack = itemEntity.getItem();
        if (itemStack.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 尝试放入容器
        Result result = tryInsertIntoContainer(container, itemStack);

        if (result.success()) {
            // 放入成功，移除实体，返回 ItemData
            entity.discard();
            ItemData resultItem = ItemData.fromContainer(
                    blockEntity.getBlockPos(),
                    world.dimension().registry().toString(),
                    result.slot());
            executor.pushData(resultItem);
        } else {
            // 放入失败，返回 false
            executor.pushData(new BooleanData(false));
        }
    }

    /**
     * 放入容器的结果
     */
    private record Result(boolean success, int slot) {
    }

    /**
     * 尝试将物品放入容器
     *
     * @param container 容器
     * @param itemStack 物品堆
     * @return Result 对象，包含是否成功和槽位索引
     */
    private Result tryInsertIntoContainer(Container container, ItemStack itemStack) {
        // 尝试 1：查找可堆叠的槽位
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existingStack = container.getItem(i);
            if (ItemStack.matches(itemStack, existingStack) &&
                    existingStack.getCount() < existingStack.getMaxStackSize()) {
                // 计算可放入的数量
                int space = existingStack.getMaxStackSize() - existingStack.getCount();
                int amountToInsert = Math.min(space, itemStack.getCount());

                existingStack.grow(amountToInsert);
                itemStack.shrink(amountToInsert);

                if (itemStack.isEmpty()) {
                    container.setChanged();
                    return new Result(true, i);
                }
            }
        }

        // 尝试 2：查找空槽位
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existingStack = container.getItem(i);
            if (existingStack.isEmpty()) {
                // 放入物品副本
                ItemStack copy = itemStack.copy();
                container.setItem(i, copy);
                itemStack.setCount(0);
                container.setChanged();
                return new Result(true, i);
            }
        }

        // 容器已满，无法放入
        return new Result(false, -1);
    }
}
