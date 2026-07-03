package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
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
                .consumesFromData("fortune", "relay:number")
                .consumesFromData("position", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器 - 通过 shellContainer 检查
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap("break_block_fortune 需要世界交互器");
            return;
        }

        ItemStack interactor = container.getInteractorStack();

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

        // 破坏方块并应用时运附魔
        // 1. 先破坏方块
        boolean destroyed = level.destroyBlock(pos, false, null, 512);  // dropResources=false，手动处理掉落
        if (!destroyed) {
            executor.pushData(new BooleanType(false));
            return;
        }

        // 2. 创建带时运附魔的假工具（使用 enchant 方法）
        ItemStack fortuneTool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        // 使用 Registry 获取附魔 - 通过 lookup 获取 HolderGetter
        var fortuneKey = net.minecraft.world.item.enchantment.Enchantments.FORTUNE;
        var holderGetter = level.registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT);
        holderGetter.ifPresent(getter -> getter.get(fortuneKey).ifPresent(holder -> 
            fortuneTool.enchant(holder, fortuneLevel)
        ));

        // 3. 手动掉落物品（应用时运）
        net.minecraft.world.level.block.Block.dropResources(state, level, pos, null, null, fortuneTool);

        executor.pushData(new BooleanType(true));
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
