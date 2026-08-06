package qdream.relay.operations.summon.shell;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EnergyException;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.errors.WorldInteractionException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.VectorData;
import qdream.relay.types.EntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;
import qdream.relay.entities.RelayEntities;
import qdream.relay.engine.Executable;

import java.util.List;

/**
 * 召唤 Shell 实体操作
 * 
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出参数：位置、coreCost、interval、range、energy、program</li>
 * <li>验证参数合法性</li>
 * <li>计算并扣除召唤者能量</li>
 * <li>生成 Shell 实体并设置配置</li>
 * <li>将程序加载到实体</li>
 * </ul>
 * 
 * <h3>能量公式</h3>
 * 
 * <pre>
 * requiredEnergy = baseCost + coreCost * 50 + (128 - range) * 2 + intervalFactor
 * 其中：
 * - baseCost = 100
 * - intervalFactor = max(0, (20 - interval) * 5)
 * </pre>
 * 
 * <h3>参数约束</h3>
 * <ul>
 * <li>coreCost: 1 ~ 8</li>
 * <li>interval: 1 ~ 100</li>
 * <li>range: 8 ~ 128</li>
 * <li>energy: >= 100 (最低启动能量)</li>
 * </ul>
 * 
 * 弹出：vector (位置), number (coreCost), number (interval), number (range), number
 * (energy), list (程序)
 * 压入：entity (召唤的实体引用，失败则为 null)
 * 
 * 需要世界交互器（用于在召唤者附近生成实体）
 */
public class SpawnShell extends Instruction {

    // 参数约束
    private static final int MIN_CORE_COST = 1;
    private static final int MAX_CORE_COST = 8;
    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 100;
    private static final int MIN_RANGE = 8;
    private static final int MAX_RANGE = 128;
    private static final double MIN_ENERGY = 100.0;
    private static final double BASE_COST = 100.0;

    public SpawnShell() {
        super("relay:spawn_shell", 1, 10, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .consumesFromData("coreCost", "relay:number")
                .consumesFromData("interval", "relay:number")
                .consumesFromData("range", "relay:number")
                .consumesFromData("energy", "relay:number")
                .consumesFromData("program", "relay:list")
                .producesToData("shell", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出参数
        VectorData posData = StackHelpers.popVector(executor, id);
        NumberData coreCostNum = StackHelpers.popNumber(executor, id);
        NumberData intervalNum = StackHelpers.popNumber(executor, id);
        NumberData rangeNum = StackHelpers.popNumber(executor, id);
        NumberData energyNum = StackHelpers.popNumber(executor, id);
        ListData programList = StackHelpers.popList(executor, id);

        // 转换为具体值
        int coreCost = coreCostNum.asInt();
        int interval = intervalNum.asInt();
        int range = rangeNum.asInt();
        double energy = energyNum.getValue();
        Vec3 position = posData.asVector();

        // 验证参数
        if (!validateParameters(coreCost, interval, range, energy, executor)) {
            return;
        }

        // 计算所需能量
        double requiredEnergy = calculateRequiredEnergy(coreCost, interval, range);

        // 检查召唤者是否有足够能量
        if (!OperationHelpers.hasEnoughEnergy(executor, requiredEnergy)) {
            throw new EnergyException(executor, "能量不足：需要 " + requiredEnergy + "，当前可用 " +
                    OperationHelpers.getAvailableEnergy(executor));
        }

        // 扣除能量
        OperationHelpers.consumeEnergy(executor, requiredEnergy);

        // 获取世界
        Level level = OperationHelpers.getLevel(executor, id).orElse(null);
        if (level == null) {
            throw new WorldInteractionException(executor, "无法获取世界");
        }

        // 生成 Shell 实体
        EntityShell shellEntity = new EntityShell(RelayEntities.ENTITY_SHELL, level);

        // 设置位置
        shellEntity.setPos(position);

        // 设置配置
        shellEntity.setCoreCost(coreCost);
        shellEntity.setInterval(interval);
        shellEntity.setRange(range);
        shellEntity.setEnergy(energy);

        // 设置 Owner
        var owner = OperationHelpers.getOwner(executor);
        if (owner == null) {
            return;
        }
        shellEntity.setOwner(owner);

        // 设置程序
        shellEntity.getStateMachine().loadProgram(programList.getValue());

        // 生成实体到世界
        if (!level.addFreshEntity(shellEntity)) {
            throw new EntityException(executor, "实体生成失败");
            // 返还能量
            // OperationHelpers.addEnergy(executor, requiredEnergy * 0.9); // 返还 90%
        }

        // 压入实体引用
        executor.pushData(EntityData.from(shellEntity, level));
    }

    /**
     * 验证参数合法性
     */
    private boolean validateParameters(int coreCost, int interval, int range, double energy, StateMachine executor) {
        if (coreCost < MIN_CORE_COST || coreCost > MAX_CORE_COST) {
            throw new ParameterException(executor,
                    "核心数量超出范围：" + coreCost + " (需要 " + MIN_CORE_COST + "-" + MAX_CORE_COST + ")");
        }

        if (interval < MIN_INTERVAL || interval > MAX_INTERVAL) {
            throw new ParameterException(executor,
                    "执行间隔超出范围：" + interval + " (需要 " + MIN_INTERVAL + "-" + MAX_INTERVAL + ")");
        }

        if (range < MIN_RANGE || range > MAX_RANGE) {
            throw new ParameterException(executor, "交互范围超出范围：" + range + " (需要 " + MIN_RANGE + "-" + MAX_RANGE + ")");
        }

        if (energy < MIN_ENERGY) {
            throw new ParameterException(executor, "预付能量不足：" + energy + " (至少需要 " + MIN_ENERGY + ")");
        }

        return true;
    }

    /**
     * 计算所需能量
     * 公式：requiredEnergy = baseCost + coreCost * 50 + (128 - range) * 2 +
     * intervalFactor
     * 其中 intervalFactor = max(0, (20 - interval) * 5)
     */
    private double calculateRequiredEnergy(int coreCost, int interval, int range) {
        double baseCost = BASE_COST;
        double coreCostFactor = coreCost * 50.0;
        double rangeFactor = (128 - range) * 2.0;
        double intervalFactor = Math.max(0, (20 - interval) * 5.0);

        return baseCost + coreCostFactor + rangeFactor + intervalFactor;
    }
}
