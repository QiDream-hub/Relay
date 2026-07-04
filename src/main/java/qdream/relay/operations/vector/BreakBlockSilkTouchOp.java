package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanData;
import qdream.relay.types.VectorData;

/**
 * 挖掘方块操作（精准采集版本）
 * 破坏指定位置的方块，应用精准采集附魔掉落方块本身
 *
 * 弹出：vector (方块位置)
 * 压入：boolean (是否成功破坏)
 *
 * 需要世界交互器，并检查范围
 */
public class BreakBlockSilkTouchOp extends Spell {

    public BreakBlockSilkTouchOp() {
        super("relay:break_block_silk_touch", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器 - 通过 shellContainer 检查
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap("break_block_silk_touch 需要世界交互器");
            return;
        }

        ItemStack interactor = container.getInteractorStack();

        // 弹出参数
        Executable posExe = executor.popData();

        if (posExe == null) {
            executor.triggerMishap("数据栈不足，需要 vector");
            return;
        }

        if (!(posExe instanceof VectorData posEx)) {
            executor.triggerMishap("期望 vector 类型");
            return;
        }

        Vec3 posVec = posEx.asVector();
        // 使用 containing 正确处理负数坐标（向下取整而非向零取整）
        BlockPos pos = BlockPos.containing(posVec);

        // 从 self 获取执行者位置作为源位置（self 可能是 Entity 或 BlockEntity）
        Vec3 sourcePos = new Vec3(0, 0, 0);
        var selfOpt = executor.getContext("self", Object.class);
        if (selfOpt.isPresent()) {
            Object self = selfOpt.get();
            if (self instanceof net.minecraft.world.entity.Entity entity) {
                sourcePos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            } else if (self instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
                net.minecraft.core.BlockPos blockPos = blockEntity.getBlockPos();
                sourcePos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            }
        }

        // 检查范围
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, posVec)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap("无法获取世界");
            return;
        }

        Level level = levelOpt.get();

        // 挖掘方块
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 破坏方块并应用精准采集附魔
        // 1. 先破坏方块
        boolean destroyed = level.destroyBlock(pos, false, null, 512);  // dropResources=false，手动处理掉落
        if (!destroyed) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 2. 创建带精准采集附魔的假工具（使用 enchant 方法）
        ItemStack silkTool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        // 使用 Registry 获取附魔 - 通过 lookup 获取 HolderGetter
        var silkTouchKey = net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
        var holderGetter = level.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT);
        holderGetter.ifPresent(getter -> getter.get(silkTouchKey).ifPresent(holder -> 
            silkTool.enchant(holder, 1)
        ));

        // 3. 手动掉落物品（应用精准采集）
        net.minecraft.world.level.block.Block.dropResources(state, level, pos, null, null, silkTool);

        executor.pushData(new BooleanData(true));
    }

    /**
     * 从上下文获取 ShellContainer
     * @param executor 状态机
     * @return ShellContainer，如果不存在返回 null
     */
    private ShellContainer getShellContainer(StateMachine executor) {
        if (!executor.hasContext("shellContainer")) {
            return null;
        }
        return executor.getContext("shellContainer", ShellContainer.class).orElse(null);
    }
}
