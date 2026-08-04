package qdream.relay.operations.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 挖掘方块操作（时运版本）
 * 破坏指定位置的方块，应用时运附魔增加掉落
 *
 * 弹出：vector (方块位置), number (时运等级)
 * 压入：boolean (是否成功破坏)
 *
 * 需要世界交互器，并检查范围
 */
public class BreakBlockFortune extends Instruction {

    public BreakBlockFortune() {
        super("relay:break_block_fortune", 1, 5, OperationSignature.builder()
                .consumesFromData("fortune", "relay:number")
                .consumesFromData("position", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        try { OperationHelpers.checkWorldInteractor(executor, id); } catch (Exception e) {
            executor.pushData(new BooleanData(false));
            return; }

        // 弹出参数
        NumberData fortune = StackHelpers.popNumber(executor, id);
        VectorData posData = StackHelpers.popVector(executor, id);

        int fortuneLevel = fortune.asInt();
        Vec3 posVec = posData.asVector();
        // 使用 containing 正确处理负数坐标（向下取整而非向零取整）
        BlockPos pos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        try { OperationHelpers.checkInRange(executor, id, sourcePos, posVec); } catch (Exception e) { 
            executor.pushData(new BooleanData(false));
            return; }

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

        // 根据时运等级额外消耗能量 (2 的 fortuneLevel 次方额外消耗)
        // 检查 fortuneLevel 范围，避免移位溢出（long 最大 63 位，double 精确表示最大 53 位）
        if (fortuneLevel < 0 || fortuneLevel > 53) {
            throw new ParameterException("时运等级超出有效范围 (0-53): " + fortuneLevel);
        }

        // 破坏方块并应用时运附魔
        // 1. 先破坏方块
        boolean destroyed = level.destroyBlock(pos, false, OperationHelpers.getOwner(executor), 512); // dropResources=false，手动处理掉落
        if (!destroyed) {
            executor.pushData(new BooleanData(false));
            return;
        }

        OperationHelpers.checkEnergy(executor, id, (double) (1L << fortuneLevel));

        // 2. 创建带时运附魔的假工具（使用 enchant 方法）
        ItemStack fortuneTool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        // 使用 Registry 获取附魔 - 通过 lookup 获取 HolderGetter
        var fortuneKey = net.minecraft.world.item.enchantment.Enchantments.FORTUNE;
        var holderGetter = level.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT);
        holderGetter.ifPresent(
                getter -> getter.get(fortuneKey).ifPresent(holder -> fortuneTool.enchant(holder, fortuneLevel)));

        // 3. 手动掉落物品（应用时运）
        net.minecraft.world.level.block.Block.dropResources(state, level, pos, null, null, fortuneTool);

        executor.pushData(new BooleanData(true));
    }
}
