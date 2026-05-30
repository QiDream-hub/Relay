package qdream.relay.client.editor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

/**
 * 栈图编辑器 Screen
 * 让玩家可视化编写程序
 */
public class SpellEditorScreen extends AbstractContainerScreen {
    
    public SpellEditorScreen(Inventory inventory, Component title) {
        super(null, inventory, title);
    }
}
