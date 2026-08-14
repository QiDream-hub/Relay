package qdream.relay.operations.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.ContainerHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.SlotData;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import qdream.relay.Relay;

/**
 * 拾取物品操作（支持跨维度）
 * 将物品实体放入 BlockEntity 容器中
 *
 * <h3>执行流程</h3>
 * <ol>
 * <li>弹出 BlockEntityData 和 EntityData</li>
 * <li>通过 worldId 分别获取对应的世界（支持跨维度）</li>
 * <li>验证实体是物品实体</li>
 * <li>通过 BlockEntityData 获取容器</li>
 * <li>尝试将物品放入容器（查找第一个可放入的空格或可堆叠槽）</li>
 * <li>放入成功：移除实体，返回 SlotData</li>
 * <li>放入失败：返回 false</li>
 * </ol>
 *
 * <h3>签名</h3>
 *
 * <pre>
 * 弹出：container (方块实体), entity (物品实体)
 * 压入：success (布尔值 false 表示失败) 或 slot (物品数据表示成功)
 * </pre>
 *
 * <h3>需求</h3>
 * <ul>
 * <li>需要世界交互器</li>
 * </ul>
 */
public class PickupItem extends Instruction {

    public PickupItem() {
        super("relay:pickup_item", 1, 2, OperationSignature.builder()
                .consumesFromData("container", "relay:block_entity")
                .consumesFromData("entity", "relay:entity")
                .producesToData("slots", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        try {
            OperationHelpers.checkWorldInteractor(executor, id);
        } catch (Exception e) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出 BlockEntityData
        BlockEntityData blockEntityData = StackHelpers.popBlockEntity(executor, id);
        if (blockEntityData == null || blockEntityData.isNull()) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_CONTAINER));
        }

        // 弹出 EntityData
        EntityData entityData = StackHelpers.popEntity(executor, id);
        if (entityData == null || entityData.isNull()) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY));
        }

        ServerLevel entityLevel = Relay.getWorld(entityData.getWorldId());
        if (entityLevel == null) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY));
        }

        // 获取 BlockEntity（使用容器所在的世界）
        var blockEntity = blockEntityData.getBlockEntity();
        if (blockEntity == null || blockEntity.isRemoved()) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_CONTAINER));
        }

        // 检查是否为 Container
        if (!(blockEntity instanceof Container container)) {
            throw new ContainerException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.NOT_A_CONTAINER));
        }

        // 获取实体（使用实体所在的世界）
        Entity entity = entityData.getEntity();
        if (entity == null || entity.isRemoved()) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY));
        }

        // 检查是否为物品实体
        if (!(entity instanceof ItemEntity itemEntity)) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY));
        }

        // 获取物品堆
        ItemStack itemStack = itemEntity.getItem();
        if (itemStack.isEmpty()) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY));
        }

        // 尝试放入容器
        var result = ContainerHelpers.tryInsertIntoContainer(container, itemStack);

        List<Executable> list = new ArrayList<>();
        for (Integer integer : result) {
            SlotData resultItem = SlotData.fromContainer(
                    blockEntity.getBlockPos(),
                    blockEntityData.getWorldId(),
                    integer);
            list.add(resultItem);
        }
        executor.pushData(new ListData(list));
    }

}
