package qdream.relay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

import qdream.relay.items.ToolShellItem;
import qdream.relay.items.ToolShellContainer;

/**
 * 注入到 ServerPlayer.tick() 中，每 tick 遍历玩家物品栏
 * 调用工具外壳的 tick 逻辑
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerTickMixin {

    @Inject(at = @At("TAIL"), method = "tick")
    private void relay$tickToolShells(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        
        // 获取玩家世界
        var world = player.level();
        if (world.isClientSide()) {
            return;
        }

        // 遍历玩家物品栏的所有插槽 (36 个插槽：主物品栏 27 + 装备栏 9)
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ToolShellItem toolShell)) {
                continue;
            }

            // 创建容器并执行 tick
            ToolShellContainer container = new ToolShellContainer(toolShell, stack);
            container.tick(world, player);
        }

        // 遍历副手插槽
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ToolShellItem toolShell) {
            ToolShellContainer container = new ToolShellContainer(toolShell, offhand);
            container.tick(world, player);
        }
    }
}
