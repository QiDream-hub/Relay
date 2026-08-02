package qdream.relay.operations.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.tools.ContainerTools;
import qdream.relay.types.SlotData;
import qdream.relay.types.VectorData;

public class PlaceBlock extends Instruction {

    public PlaceBlock() {
        super("relay:place_block", 1, 1, OperationSignature.builder()
                .consumesFromData("item", "relay:slot")
                .consumesFromData("vector", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }
        SlotData popSlot = StackHelpers.popSlot(executor, id);
        VectorData popVector = StackHelpers.popVector(executor, id);
        if (popSlot == null || popVector == null) {
            return;
        }
        ItemStack itemStack = ContainerTools.getItemStack(popSlot);
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem blockItem)) {
            executor.triggerMishap("错误的物品");
            return;
        }

        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty())
            return;

        Level level = levelOpt.get();

        Vec3 vec3 = popVector.getVec3();

        // 获取源位置用于计算放置方向
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);

        // 检查放置地点是否在范围内
        if (!OperationHelpers.checkInRange(executor, id, sourcePos, vec3)) {
            return;
        }
        // // 检查容器是否在范围内
        // if (!OperationHelpers.checkInRange(executor, id, sourcePos, popSlot.getContainerPos().getCenter())) {
        //     return;
        // }

        // 使用 containing 正确处理负数坐标（向下取整而非向零取整）
        BlockPos pos = BlockPos.containing(vec3);

        // 计算从源位置指向目标位置的方向（使用 Vec3i 重载版本）
        Direction direction = Direction.getNearest(pos.subtract(BlockPos.containing(sourcePos)), Direction.UP);

        // 创建 BlockHitResult 用于模拟玩家放置
        BlockHitResult hitResult = new BlockHitResult(vec3, direction, pos, false);

        // 创建 BlockPlaceContext
        BlockPlaceContext context = new BlockPlaceContext(
                level,
                null, // 玩家为 null（无主操作）
                InteractionHand.MAIN_HAND,
                itemStack,
                hitResult);

        // 使用 BlockItem 的放置逻辑（处理朝向、特殊方块等）
        blockItem.useOn(context);
    }

}
