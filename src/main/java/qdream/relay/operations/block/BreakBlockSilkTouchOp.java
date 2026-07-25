package qdream.relay.operations.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
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
        super("relay:break_block_silk_touch", 1, 20, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出参数
        VectorData posData = StackHelpers.popVector(executor, id);
        if (posData == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 posVec = posData.asVector();
        // 使用 containing 正确处理负数坐标（向下取整而非向零取整）
        BlockPos pos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        if (!OperationHelpers.checkInRange(executor, id, sourcePos, posVec)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) {
            executor.pushData(new BooleanData(false));
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
        ItemStack silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
        // 使用 Registry 获取附魔 - 通过 lookup 获取 HolderGetter
        var silkTouchKey = Enchantments.SILK_TOUCH;
        var holderGetter = level.registryAccess().lookup(Registries.ENCHANTMENT);
        holderGetter.ifPresent(getter -> getter.get(silkTouchKey).ifPresent(holder -> 
            silkTool.enchant(holder, 1)
        ));

        // 3. 手动掉落物品（应用精准采集）
        Block.dropResources(state, level, pos, null, null, silkTool);

        executor.pushData(new BooleanData(true));
    }
}
