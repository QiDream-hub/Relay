package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanType;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

/**
 * 挖掘方块操作（时运版本）
 * 破坏指定位置的方块，应用时运附魔增加掉落
 *
 * 弹出：vector (方块位置), number (时运等级)
 * 压入：boolean (是否成功破坏)
 *
 * 需要世界交互器，并检查范围
 */
public class BreakBlockFortuneOp extends Spell {

    public BreakBlockFortuneOp() {
        super("relay:break_block_fortune", 2, 1, OperationSignature.builder()
                .consumesFromData("relay:number")
                .consumesFromData("relay:vector")
                .producesToData("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasContext("worldInteractor")) {
            executor.triggerMishap("break_block_fortune 需要世界交互器");
            return;
        }

        Optional<ItemStack> interactorOpt = executor.getContext("worldInteractor", ItemStack.class);
        if (interactorOpt.isEmpty() || interactorOpt.get().isEmpty()) {
            executor.triggerMishap("世界交互器无效");
            return;
        }

        ItemStack interactor = interactorOpt.get();

        // 弹出参数
        Executable fortuneExe = executor.popData();
        Executable posExe = executor.popData();

        if (fortuneExe == null || posExe == null) {
            executor.triggerMishap("数据栈不足，需要 number, vector");
            return;
        }

        if (!(fortuneExe instanceof NumberType fortuneEx) ||
            !(posExe instanceof VectorType posEx)) {
            executor.triggerMishap("期望 number, vector 类型");
            return;
        }

        int fortuneLevel = (int) fortuneEx.asDouble();
        Vec3 posVec = posEx.asVector();
        BlockPos pos = new BlockPos((int) posVec.x, (int) posVec.y, (int) posVec.z);

        // 获取世界交互器位置（从上下文或默认原点）
        Vec3 sourcePos = new Vec3(0, 0, 0);
        Optional<Vec3> sourceOpt = executor.getContext("sourcePos", Vec3.class);
        if (sourceOpt.isPresent()) {
            sourcePos = sourceOpt.get();
        }

        // 检查范围
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, posVec)) {
            executor.pushData(new BooleanType(false));
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
            executor.pushData(new BooleanType(false));
            return;
        }

        // 破坏方块，应用时运附魔
        // 使用破坏方块并掉落物品的方式
        boolean destroyed = level.destroyBlock(pos, true, null, fortuneLevel);
        executor.pushData(new BooleanType(destroyed));
    }
}
