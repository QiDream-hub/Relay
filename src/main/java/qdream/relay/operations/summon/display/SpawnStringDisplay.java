package qdream.relay.operations.summon.display;

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
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.VectorData;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.types.StringData;
import qdream.relay.types.BooleanData;
import qdream.relay.entities.StringDisplay;
import qdream.relay.entities.RelayEntities;

/**
 * 召唤 StringDisplay 实体操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出参数：位置、文本、能量、是否穿墙、是否追踪玩家、朝向目标点</li>
 * <li>验证参数合法性</li>
 * <li>计算并扣除召唤者能量</li>
 * <li>生成 StringDisplay 实体并设置配置</li>
 * </ul>
 *
 * <h3>能量公式</h3>
 *
 * <pre>
 * requiredEnergy = baseCost + textLength * 2 + durationFactor
 * 其中：
 * - baseCost = 50
 * - durationFactor = energy * 0.1 (基于提供的能量值)
 * </pre>
 *
 * <h3>默认配置</h3>
 * <ul>
 * <li>文本颜色：黑色 (0x000000)</li>
 * <li>背景：不渲染 (透明度 0)</li>
 * <li>穿墙：false</li>
 * <li>追踪玩家：true (BillboardType.CENTER)</li>
 * <li>朝向目标点：null (不设置朝向，使用追踪玩家模式)</li>
 * </ul>
 *
 * 弹出：vector (位置), vector (朝向), string (文本), number (能量), boolean (是否穿墙),
 * boolean (是否追踪玩家)
 * 
 * 压入：entity (召唤的实体引用，失败则为 null)
 *
 * 需要世界交互器（用于在召唤者附近生成实体）
 */
public class SpawnStringDisplay extends Instruction {

    public SpawnStringDisplay() {
        super("relay:spawn_string_display", 1, 10, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .consumesFromData("lookDirection", "relay:vector")
                .consumesFromData("text", "relay:string")
                .consumesFromData("energy", "relay:number")
                .consumesFromData("see_through", "relay:boolean")
                .consumesFromData("track_player", "relay:boolean")
                .producesToData("entity", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出参数
        VectorData posData = StackHelpers.popVector(executor, id);
        VectorData lookAtData = StackHelpers.popVector(executor, id);
        StringData textData = StackHelpers.popString(executor, id);
        NumberData energyNum = StackHelpers.popNumber(executor, id);
        BooleanData seeThroughData = StackHelpers.popBoolean(executor, id);
        BooleanData trackPlayerData = StackHelpers.popBoolean(executor, id);

        // 转换为具体值
        String text = textData.getValue();
        double energy = energyNum.getValue();
        boolean seeThrough = seeThroughData.asBoolean();
        boolean trackPlayer = trackPlayerData.asBoolean();
        Vec3 position = posData.asVector();
        Vec3 lookAt = lookAtData.asVector();

        // 验证参数
        if (text.isEmpty()) {
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.TEXT_EMPTY));
        }

        // 检查召唤者是否有足够能量
        if (!OperationHelpers.consumeEnergy(executor, energyNum.getValue())) {
            throw new EnergyException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.ENERGY_INSUFFICIENT, energyNum.getValue()));
        }

        // 获取世界
        Level level = OperationHelpers.getLevel(executor, id).orElse(null);
        if (level == null) {
            throw new WorldInteractionException(executor,
                    ErrorMessageTools.buildErrorMessage(ErrorType.WORLD_NOT_AVAILABLE));
        }

        // 生成 StringDisplay 实体
        StringDisplay display = new StringDisplay(RelayEntities.STRING_DISPLAY, level);

        // 设置位置
        display.setPos(position);

        // 设置文本
        display.setTextString(text);

        // 设置文本颜色
        display.setTextColor(0xFFFFFFFF); // 不透明纯白

        // 设置背景（默认不渲染，透明度 0）
        display.setBackgroundAlpha(0);

        // 设置穿墙属性
        display.setSeeThrough(seeThrough);

        // 设置朝向或 Billboard 约束
        if (lookAt != null && !lookAt.equals(Vec3.ZERO)) {
            // 如果提供了有效的朝向目标点，设置朝向并覆盖 Billboard 为 FIXED
            display.lookAt(lookAt, true);
        } else if (trackPlayer) {
            // 如果没有设置朝向且追踪玩家，使用 CENTER 模式
            display.setBillboardConstraints(StringDisplay.BillboardType.CENTER);
        } else {
            // 否则使用 FIXED 模式（默认朝向）
            display.setBillboardConstraints(StringDisplay.BillboardType.FIXED);
        }

        // 设置能量
        display.setEnergy(energy);

        // 生成实体到世界
        if (!level.addFreshEntity(display)) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ENTITY_SPAWN_FAILED));
        }

        // 压入实体引用
        executor.pushData(EntityData.from(display, level));
    }
}
