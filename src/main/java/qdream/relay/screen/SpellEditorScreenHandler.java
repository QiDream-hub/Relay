package qdream.relay.screen;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import qdream.relay.blocks.entity.SpellEditorBlockEntity;
import qdream.relay.mc.OperationRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 法术编辑器 Screen Handler
 * 管理编辑器的服务端状态
 */
public class SpellEditorScreenHandler extends AbstractContainerMenu {

    /**
     * 当前编辑的程序列表（操作 ID 序列）
     */
    private final List<String> program;

    /**
     * 所有可用的操作 ID
     */
    private final List<String> availableOperations;

    /**
     * 方块实体引用（用于同步）
     */
    private final SpellEditorBlockEntity blockEntity;

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory, SpellEditorBlockEntity blockEntity) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.program = new ArrayList<>();
        this.availableOperations = new ArrayList<>(OperationRegistry.getAllIds());
        
        // 调试日志
        System.out.println("[SpellEditorScreenHandler] 初始化，可用操作数量：" + availableOperations.size());
        for (String op : availableOperations) {
            System.out.println("  - " + op);
        }

        // 玩家物品栏
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 170 + y * 18));
            }
        }

        // 玩家热键栏
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 248));
        }
    }

    /**
     * 获取程序列表
     */
    public List<String> getProgram() {
        return program;
    }

    /**
     * 设置程序列表
     */
    public void setProgram(List<String> program) {
        this.program.clear();
        this.program.addAll(program);
    }

    /**
     * 添加操作到程序
     */
    public void addOperation(String opId) {
        if (OperationRegistry.contains(opId)) {
            this.program.add(opId);
        }
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
     * 清空程序
     */
    public void clearProgram() {
        this.program.clear();
    }

    /**
     * 获取所有可用操作
     */
    public List<String> getAvailableOperations() {
        return availableOperations;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
