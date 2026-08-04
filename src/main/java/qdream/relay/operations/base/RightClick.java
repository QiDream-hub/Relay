package qdream.relay.operations.base;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;
import qdream.relay.types.SlotData;
import qdream.relay.types.VectorData;

/**
 * 右键点击操作
 * 模拟玩家右键点击方块
 *
 * 弹出：
 * - entity (点击者实体，必须是玩家)
 * - item (物品槽)
 * - target (目标位置)
 * - face (点击的面方向，使用向量表示)
 * 压入：boolean (是否成功交互)
 *
 * 需要世界交互器，并检查范围
 */
public class RightClick extends Instruction {

    public RightClick() {
        super("relay:right_click", 1, 5, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("item", "relay:slot")
                .consumesFromData("target", "relay:vector")
                .consumesFromData("face", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        try { OperationHelpers.checkWorldInteractor(executor, id); } catch (Exception e) { 
            executor.pushData(new BooleanData(false));
            return; }

        EntityData entityData = StackHelpers.popEntity(executor, id);
        SlotData slotData = StackHelpers.popSlot(executor, id);
        VectorData targetData = StackHelpers.popVector(executor, id);
        VectorData faceData = StackHelpers.popVector(executor, id);

        // 获取实体并检查是否为玩家
        Entity entity = entityData.getEntity();
        if (entity == null || !(entity instanceof Player player)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取物品堆

        ItemStack itemStack = slotData.getItemStack();

        if (itemStack.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 hitPos = targetData.getVec3();
        Vec3 sourcePos = entity.position();

        // 检查范围
        try { OperationHelpers.checkInRange(executor, id, sourcePos, hitPos); } catch (Exception e) { 
            executor.pushData(new BooleanData(false));
            return; }

        // 使用击中点计算方块位置
        // 从向量转换为方向
        Direction face = OperationHelpers.getDirectionFromVector(faceData.getVec3());

        // 如果击中点正好在方块边界上（如顶面 Y=整数），containing 可能返回错误的方块
        // 需要稍微向内偏移，确保 containing 返回正确的方块位置
        Vec3 adjustedHitPos = hitPos;
        if (face != null) {
            // 沿 face 反方向偏移 0.001，确保点在方块内部而不是表面上
            adjustedHitPos = hitPos.subtract(face.getStepX() * 0.001,
                    face.getStepY() * 0.001,
                    face.getStepZ() * 0.001);
        }
        BlockPos blockPos = BlockPos.containing(adjustedHitPos);

        // 创建 BlockHitResult - 使用原始击中点和计算出的方块位置
        // 击中点必须在方块表面上，这样 HoeItem 等才能正确工作
        BlockHitResult blockHitResult = new BlockHitResult(
                hitPos,
                face,
                blockPos,
                false);

        // 设置玩家手中的物品
        // player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Level level = levelOpt.get();

        // 创建 UseOnContext 用于右键点击方块
        UseOnContext useOnContext = new UseOnContext(
                level,
                player,
                InteractionHand.MAIN_HAND,
                itemStack,
                blockHitResult);

        // 使用物品点击方块
        InteractionResult blockResult = itemStack.useOn(useOnContext);

        if (blockResult.consumesAction()) {
            // useOn 可能修改了物品（如减少数量、增加耐久度），需要同步回容器
            OperationHelpers.updateContainerItem(slotData, itemStack);
            executor.pushData(new BooleanData(true));
            return;
        }

        // 如果方块交互失败，尝试使用物品（例如使用食物、药水等）
        InteractionResult useResult = itemStack.use(level, player, InteractionHand.MAIN_HAND);
        if (useResult.consumesAction()) {
            OperationHelpers.updateContainerItem(slotData, itemStack);
            executor.pushData(new BooleanData(true));
            return;
        }

        executor.pushData(new BooleanData(false));
    }
}
