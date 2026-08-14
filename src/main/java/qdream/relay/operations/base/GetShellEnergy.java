package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ContainerException;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.entities.EntityShell;

/**
 * 获取 Shell 能量操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 Shell 引用（实体或方块实体）</li>
 * <li>验证 Shell 是否有效</li>
 * <li>获取 Shell 当前能量值并压入数据栈</li>
 * </ul>
 *
 * 弹出：shell (entity/block_entity)
 * 压入：number (当前能量值)
 */
public class GetShellEnergy extends Instruction {

    public GetShellEnergy() {
        super("relay:get_shell_energy", 1, 1, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity", "relay:block_entity")
                .producesToData("energyCurrent", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出 Shell 引用（可能是实体或方块实体）
        var shellData = executor.popData();

        double energy;

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

            energy = shell.getEnergy();

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

            energy = shell.getEnergy();

        } else {
            throw new EntityException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.TYPE_MISMATCH,
                            "Shell", shellData.getClass().getSimpleName()));
        }

        // 压入能量值
        executor.pushData(new NumberData(energy));
    }
}
