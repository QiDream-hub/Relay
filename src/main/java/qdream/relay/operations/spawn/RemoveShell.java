package qdream.relay.operations.spawn;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;

/**
 * 移除 Shell 实体操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 EntityData（Shell 实体引用）</li>
 * <li>验证实体是否为 Shell 实体</li>
 * <li>验证调用者是否为 Shell 的 Owner</li>
 * <li>移除 Shell 实体并返还剩余能量</li>
 * </ul>
 *
 * <h3>能量返还</h3>
 * <p>移除 Shell 时，将 Shell 当前能量的 90% 返还给调用者（10% 作为手续费）</p>
 *
 * 弹出：entity (要移除的 Shell 实体)
 * 压入：number (返还的能量值，失败则为 0)
 *
 * 需要世界交互器（用于验证和移除实体）
 */
public class RemoveShell extends Instruction {

    private static final double ENERGY_RETURN_RATIO = 0.9;

    public RemoveShell() {
        super("relay:remove_shell", 1, 2, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .producesToData("energy", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        // 弹出实体参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        if (entityData == null)
            return;

        // 获取实体
        var entity = entityData.getEntity();
        if (entity == null) {
            executor.triggerMishap("实体引用无效");
            return;
        }

        // 验证是否为 Shell 实体
        if (!(entity instanceof EntityShell shellEntity)) {
            executor.triggerMishap("目标实体不是 Shell 实体");
            return;
        }

        // 验证调用者是否为 Owner
        var owner = OperationHelpers.getOwner(executor);
        if (owner == null) {
            executor.triggerMishap("无法获取调用者");
            return;
        }

        var shellOwner = shellEntity.getOwner();
        if (shellOwner == null || !shellOwner.getUUID().equals(owner.getUUID())) {
            executor.triggerMishap("无权移除此 Shell（不属于你）");
            return;
        }

        // 获取 Shell 剩余能量
        double remainingEnergy = shellEntity.getEnergy();

        // 移除实体
        Level level = entity.level();
        if (!level.isClientSide()) {
            shellEntity.remove(Entity.RemovalReason.DISCARDED);
        }

        // 返还能量（90%）
        double returnEnergy = remainingEnergy * ENERGY_RETURN_RATIO;
        OperationHelpers.addEnergy(executor, returnEnergy);

        // 压入返还的能量值
        executor.pushData(new NumberData(returnEnergy));
    }
}
