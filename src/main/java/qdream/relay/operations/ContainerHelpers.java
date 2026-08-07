package qdream.relay.operations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import qdream.relay.Relay;
import qdream.relay.types.SlotData;

import java.util.ArrayList;
import java.util.List;

/**
 * 容器操作工具类
 * 提供容器和物品堆的获取、合并、查询等操作
 */
public class ContainerHelpers {
    private ContainerHelpers() {
    }

    /**
     * 根据 SlotData 获取容器
     * 通过 BlockPos + 世界 ID 获取方块容器
     *
     * @param slotData 槽位数据
     * @return 容器实例，如果找不到返回 null
     */
    public static Container getContainer(SlotData slotData) {
        if (slotData == null) {
            return null;
        }

        String worldId = slotData.getWorldId();
        if (worldId == null) {
            return null;
        }

        Level level = Relay.getWorld(worldId);
        if (level == null) {
            return null;
        }

        BlockPos containerPos = slotData.getContainerPos();
        if (containerPos != null) {
            var blockEntity = level.getBlockEntity(containerPos);
            if (blockEntity instanceof Container container) {
                return container;
            }
        }

        return null;
    }

    /**
     * 根据 SlotData 获取物品堆
     *
     * @param slotData 槽位数据
     * @return 物品堆，如果找不到返回 ItemStack.EMPTY
     */
    public static ItemStack getItemStack(SlotData slotData) {
        if (slotData == null) {
            return ItemStack.EMPTY;
        }

        Container container = getContainer(slotData);
        if (container == null) {
            return ItemStack.EMPTY;
        }

        int slot = slotData.getSlot();
        if (slot < 0 || slot >= container.getContainerSize()) {
            return ItemStack.EMPTY;
        }

        return container.getItem(slot);
    }

    /**
     * 尝试将物品放入容器
     * 先尝试放入可堆叠的槽位中，再尝试放入空的槽位，返回实际放入的槽位列表
     *
     * @param container 容器
     * @param itemStack 物品堆（会被修改）
     * @return 槽位索引列表，按放入顺序排列
     */
    public static List<Integer> tryInsertIntoContainer(Container container, ItemStack itemStack) {
        List<Integer> slots = new ArrayList<>();

        // 第一阶段：尝试放入可堆叠的槽位
        for (int i = 0; i < container.getContainerSize() && !itemStack.isEmpty(); i++) {
            ItemStack existingStack = container.getItem(i);
            if (!existingStack.isEmpty() && ItemStack.isSameItemSameComponents(existingStack, itemStack)) {
                int maxStackSize = Math.min(container.getMaxStackSize(existingStack), existingStack.getMaxStackSize());
                int space = maxStackSize - existingStack.getCount();

                if (space > 0) {
                    int amountToInsert = Math.min(space, itemStack.getCount());
                    existingStack.grow(amountToInsert);
                    itemStack.shrink(amountToInsert);
                    slots.add(i);
                    container.setChanged();
                }
            }
        }

        // 第二阶段：尝试放入空槽位
        for (int i = 0; i < container.getContainerSize() && !itemStack.isEmpty(); i++) {
            ItemStack existingStack = container.getItem(i);
            if (existingStack.isEmpty()) {
                int maxStackSize = Math.min(container.getMaxStackSize(itemStack), itemStack.getMaxStackSize());
                int amountToInsert = Math.min(maxStackSize, itemStack.getCount());

                container.setItem(i, itemStack.split(amountToInsert));
                slots.add(i);
                container.setChanged();
            }
        }

        return slots;
    }

    /**
     * 获取容器内的所有物品
     *
     * @param container    容器
     * @param containerPos 容器位置
     * @param world        世界
     * @return ItemData 列表，包含容器中的所有非空物品
     */
    public static List<SlotData> getContainerItems(Container container, BlockPos containerPos, Level world) {
        List<SlotData> result = new ArrayList<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                result.add(SlotData.from(containerPos, world, i));
            }
        }

        return result;
    }

    /**
     * 尝试将源物品移动到目标槽位
     * 支持堆叠到已有物品或放入空槽
     *
     * @param targetContainer 目标容器
     * @param targetSlot      目标槽位
     * @param sourceContainer 源容器
     * @param sourceSlot      源槽位
     * @return 源物品剩余的数量，0 表示完全移动
     */
    public static int moveItems(
            Container targetContainer, int targetSlot,
            Container sourceContainer, int sourceSlot) {
        ItemStack targetStack = targetContainer.getItem(targetSlot);
        ItemStack sourceStack = sourceContainer.getItem(sourceSlot);

        if (sourceStack.isEmpty()) {
            return 0;
        }

        // 如果目标槽位为空，尝试直接放入
        if (targetStack.isEmpty()) {
            int maxStackSize = Math.min(
                    targetContainer.getMaxStackSize(sourceStack),
                    sourceStack.getMaxStackSize());
            int amountToMove = Math.min(maxStackSize, sourceStack.getCount());
            
            targetContainer.setItem(targetSlot, sourceStack.split(amountToMove));
            targetContainer.setChanged();
            sourceContainer.setChanged();
            return sourceStack.getCount();
        }

        // 检查是否可以堆叠（同物品、同 NBT、同组件）
        if (!ItemStack.isSameItemSameComponents(targetStack, sourceStack)) {
            return sourceStack.getCount();
        }

        // 计算目标槽位的剩余空间
        int maxStackSize = Math.min(
                Math.min(targetContainer.getMaxStackSize(targetStack), targetStack.getMaxStackSize()),
                Math.min(sourceContainer.getMaxStackSize(sourceStack), sourceStack.getMaxStackSize()));
        int spaceInTarget = maxStackSize - targetStack.getCount();

        if (spaceInTarget <= 0) {
            // 目标槽位已满
            return sourceStack.getCount();
        }

        // 执行堆叠
        if (sourceStack.getCount() <= spaceInTarget) {
            // 源物品可以完全合并到目标
            targetStack.grow(sourceStack.getCount());
            sourceContainer.setItem(sourceSlot, ItemStack.EMPTY);
            targetContainer.setChanged();
            sourceContainer.setChanged();
            return 0;
        } else {
            // 只能部分合并
            targetStack.grow(spaceInTarget);
            sourceStack.shrink(spaceInTarget);
            targetContainer.setChanged();
            sourceContainer.setChanged();
            return sourceStack.getCount();
        }
    }

    /**
     * 获取物品数量
     *
     * @param itemData 物品数据
     * @param world    世界
     * @return 物品数量，如果物品不存在返回 0
     */
    public static int getItemCount(SlotData itemData) {
        if (itemData == null) {
            return 0;
        }

        ItemStack stack = itemData.getItemStack();
        if (stack.isEmpty()) {
            return 0;
        }

        return stack.getCount();
    }

    /**
     * 在指定位置生成物品实体
     *
     * @param world     世界
     * @param pos       位置
     * @param itemStack 物品堆
     * @return 生成的 ItemEntity，如果失败返回 null
     */
    public static ItemEntity spawnItemEntity(Level world, BlockPos pos, ItemStack itemStack) {
        if (world == null || itemStack == null || itemStack.isEmpty()) {
            return null;
        }

        // 在方块中心上方生成
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        ItemEntity entity = new ItemEntity(world, x, y, z, itemStack.copy());

        // 设置随机运动
        entity.setDeltaMovement(
                world.getRandom().nextGaussian() * 0.1,
                world.getRandom().nextGaussian() * 0.1 + 0.2,
                world.getRandom().nextGaussian() * 0.1);

        if (!world.isClientSide()) {
            world.addFreshEntity(entity);
        }

        return entity;
    }
}
