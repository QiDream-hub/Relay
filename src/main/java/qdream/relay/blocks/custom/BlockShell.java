package qdream.relay.blocks.custom;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.core.ShellCoreGroupManager;
import qdream.relay.screen.BlockShellScreenHandler;

/**
 * 外壳方块
 * 容器，决定形态为方块
 * 支持红石信号激活：红石信号激活时切换 enabled 属性，不限制面数
 */
public class BlockShell extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public BlockShell(Block.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(BlockShell::new);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, net.minecraft.world.level.redstone.@Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }

        boolean powered = (Boolean) state.getValue(POWERED);
        boolean receivingPower = level.hasNeighborSignal(pos);

        if (powered && !receivingPower) {
            // 红石信号关闭，仅更新状态
            level.setBlock(pos, state.setValue(POWERED, false), 2);
        } else if (!powered && receivingPower) {
            // 红石信号激活，切换 enabled 属性
            level.setBlock(pos, state.setValue(POWERED, true), 2);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BlockShellEntity shell) {
                shell.toggleEnabled();
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockShellEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, RelayBlockEntities.SHELL_BLOCK_ENTITY, BlockShellEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BlockShellEntity shell) {
                if (!(placer instanceof Player player)) {
                    shell.setOwner(null);
                } else {
                    shell.setOwner(player);
                }

                // 检测相邻方块并加入核心共享组（6 向检测）
                ShellCoreGroupManager.onBlockPlaced(world, pos, shell);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (world.isClientSide()) {
            // 客户端保存最后交互的方块坐标，用于 GUI 打开时获取正确的 blockPos
            BlockShellScreenHandler.setLastKnownBlockPos(pos);
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof BlockShellEntity shell) {
            player.openMenu(shell);
        }

        return InteractionResult.CONSUME;
    }

}
