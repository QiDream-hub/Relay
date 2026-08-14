package qdream.relay.operations.base;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.core.EnergySystem;
import qdream.relay.engine.StateMachine;
import qdream.relay.entities.EntityShell;
import qdream.relay.items.EnergyModuleItem;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.EntityData;
import qdream.relay.types.SlotData;

/**
 * 从插槽物品为 Shell 添加能量操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 Shell 引用（实体或方块实体）和插槽引用</li>
 * <li>从插槽获取物品堆</li>
 * <li>如果物品是紫水晶（碎片/块），消耗物品并为 Shell 添加能量</li>
 * <li>如果物品是能量模块，移除模块中的全部能量并为 Shell 添加</li>
 * </ul>
 *
 * 弹出：shell (entity/block_entity), slot (relay:slot)
 * 压入：number (实际添加的能量值)
 */
public class AddShellEnergyFromSlot extends Instruction {

    public AddShellEnergyFromSlot() {
        super("relay:add_shell_energy_from_slot", 1, 5, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity", "relay:block_entity")
                .consumesFromData("slot", "relay:slot")
                .producesToData("energyAdded", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出 Shell 引用（可能是实体或方块实体）
        var shellData = executor.popData();
        SlotData slotData = StackHelpers.popSlot(executor, id);

        // 获取插槽中的物品
        ItemStack itemStack = slotData.getItemStack();
        if (itemStack.isEmpty()) {
            executor.pushData(new qdream.relay.types.NumberData(0));
            return;
        }

        double energyToAdd = 0;

        // 检查是否为紫水晶（碎片/块）
        if (EnergySystem.hasEnergy(itemStack)) {
            energyToAdd = EnergySystem.getEnergyValue(itemStack);

            // 从容器中移除物品
            BlockEntity containerBlockEntity = slotData.getContainer();
            if (containerBlockEntity instanceof Container container) {
                int slot = slotData.getSlot();
                if (slot < container.getContainerSize()) {
                    container.removeItem(slot, itemStack.getCount());
                    containerBlockEntity.setChanged();
                }
            }

        } else if (itemStack.getItem() instanceof EnergyModuleComponent energyModule) {
            // 如果是能量模块，提取全部能量
            energyToAdd = energyModule.getStoredEnergy(itemStack);

            if (energyToAdd > 0) {
                // 清空能量模块的能量
                energyModule.consumeEnergy(itemStack, energyToAdd);
            }
        }

        if (energyToAdd <= 0) {
            executor.pushData(new qdream.relay.types.NumberData(0));
            return;
        }

        // 为 Shell 添加能量
        if (shellData instanceof EntityData entityData) {
            // 实体 Shell
            var entity = entityData.getEntity();
            if (entity == null) {
                throw new EntityException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.ENTITY_REFERENCE_INVALID));
            }

            if (!(entity instanceof EntityShell shell)) {
                throw new EntityException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.NOT_ENTITY_SHELL));
            }

            shell.addEnergy(energyToAdd);

        } else if (shellData instanceof BlockEntityData blockEntityData) {
            // 方块 Shell
            var blockEntity = blockEntityData.getBlockEntity();
            if (blockEntity == null) {
                throw new ContainerException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.BLOCK_ENTITY_REFERENCE_INVALID));
            }

            if (!(blockEntity instanceof BlockShellEntity shell)) {
                throw new ContainerException(executor,
                        ErrorMessageTools.buildErrorMessage(ErrorType.NOT_A_CONTAINER));
            }

            shell.addEnergy(energyToAdd);

        } else {
            throw new EntityException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.TYPE_MISMATCH,
                            "Shell", shellData.getClass().getSimpleName()));
        }

        // 返回实际添加的能量值
        executor.pushData(new qdream.relay.types.NumberData(energyToAdd));
    }
}
