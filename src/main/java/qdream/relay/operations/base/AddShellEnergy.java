package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.errors.EnergyException;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.entities.EntityShell;

/**
 * 为 Shell 添加能量操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 Shell 引用（实体或方块实体）和能量值</li>
 * <li>验证 Shell 是否有效</li>
 * <li>从执行者能量池中扣除能量并为 Shell 添加</li>
 * </ul>
 *
 * <h3>参数约束</h3>
 * <ul>
 * <li>energy: &gt; 0</li>
 * </ul>
 *
 * 弹出：shell (entity/block_entity), number (能量值)
 * 无输出
 */
public class AddShellEnergy extends Instruction {

    public AddShellEnergy() {
        super("relay:add_shell_energy", 1, 5, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity", "relay:block_entity")
                .consumesFromData("energy", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出 Shell 引用（可能是实体或方块实体）
        var shellData = executor.popData();
        NumberData energyNum = StackHelpers.popNumber(executor, id);

        double energy = energyNum.getValue();

        // 验证能量值
        if (energy <= 0) {
            throw new ParameterException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.ENERGY_MUST_BE_POSITIVE));
        }

        // 检查执行者能量是否足够
        if (!OperationHelpers.consumeEnergy(executor, energy)) {
            throw new EnergyException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.ENERGY_INSUFFICIENT, energy));
        }

        // 根据类型处理
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

            // 添加能量
            shell.addEnergy(energy);

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

            // 添加能量
            shell.addEnergy(energy);

        } else {
            throw new EntityException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.TYPE_MISMATCH,
                            "Shell", shellData.getClass().getSimpleName()));
        }
    }
}
