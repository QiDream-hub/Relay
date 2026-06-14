package qdream.relay.blocks.custom;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;

import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.blocks.entity.custom.SpellEditorBlockEntity;

/**
 * 法术编辑器方块
 * 右键打开编辑器界面
 */
public class SpellEditorBlock extends BaseEntityBlock {

    public SpellEditorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpellEditorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 服务端打开编辑器菜单
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SpellEditorBlockEntity editor) {
            player.openMenu(editor);
        }

        return InteractionResult.CONSUME;
    }
}
