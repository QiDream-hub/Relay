package qdream.relay.blocks.entity.custom;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.screen.SpellEditorScreenHandler;

/**
 * 法术编辑器方块实体
 * 实现 MenuProvider 接口以支持 GUI 打开
 */
public class SpellEditorBlockEntity extends BlockEntity implements MenuProvider {

    public SpellEditorBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SPELL_EDITOR_BLOCK_ENTITY, pos, state);
    }

    // ========== MenuProvider 接口 ==========

    @Override
    public Component getDisplayName() {
        return Component.literal("法术编辑器");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new SpellEditorScreenHandler(syncId, inv, this);
    }
}
