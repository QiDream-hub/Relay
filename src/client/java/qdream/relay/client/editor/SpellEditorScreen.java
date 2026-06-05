package qdream.relay.client.editor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

import qdream.relay.client.networking.RelayClientNetworking;

/**
 * 栈图编辑器 Screen
 * 让玩家可视化编写程序
 * 注意：26.1.2 的 GUI 系统有变化，这里暂时简化实现
 */
public class SpellEditorScreen extends AbstractContainerScreen {

    private List<String> availableOperations;
    private List<String> program;

    public SpellEditorScreen(Inventory inventory, Component title) {
        super(null, inventory, title);
        this.availableOperations = new ArrayList<>();
        this.program = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        
        // 获取可用的操作列表
        this.availableOperations = RelayClientNetworking.getAvailableOperations();
    }

    /**
     * 添加操作到程序
     */
    public void addOperation(String opId) {
        this.program.add(opId);
    }

    /**
     * 从程序移除操作
     */
    public void removeOperation(int index) {
        if (index >= 0 && index < this.program.size()) {
            this.program.remove(index);
        }
    }

    /**
     * 获取当前程序
     */
    public List<String> getProgram() {
        return new ArrayList<>(this.program);
    }

    /**
     * 设置程序
     */
    public void setProgram(List<String> program) {
        this.program = new ArrayList<>(program);
    }
}
