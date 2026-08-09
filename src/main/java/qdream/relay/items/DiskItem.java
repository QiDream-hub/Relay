package qdream.relay.items;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.function.Consumer;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.tools.TextTools;

/**
 * 法术磁盘物品
 * 存储栈图程序（Iota 列表）
 * 使用 26.1.2 DataComponent 系统，底层使用 NBT 格式存储
 */
public class DiskItem extends Item implements DiskComponent {

    public DiskItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
            Consumer<Component> textConsumer, TooltipFlag type) {
        if (!hasProgram(stack)) {
            textConsumer.accept(Component.translatable("item.relay.spell_disk.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        int size = getProgramSize(stack);
        textConsumer.accept(
                Component.translatable("item.relay.spell_disk.size", size).withStyle(ChatFormatting.GOLD));

        // 显示程序内容，自动换行
        ListTag programTag = getProgram(stack);
        if (programTag != null && !programTag.isEmpty()) {
            // 最多显示 8 行，避免工具提示过长
            int maxLines = 8;
            int displayCount = Math.min(programTag.size(), maxLines);

            for (int i = 0; i < displayCount; i++) {
                textConsumer.accept(
                        Component.literal("  • " + TextTools.getName(programTag, i)).withStyle(ChatFormatting.GRAY));
            }

            // 如果程序超过显示行数，显示省略提示
            if (programTag.size() > maxLines) {
                int remaining = programTag.size() - maxLines;
                textConsumer.accept(Component.translatable("item.relay.spell_disk.more", remaining)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public ListTag getProgram(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return new ListTag();
        }
        return programTag.getList("program").orElse(new ListTag());
    }

    @Override
    public void setProgram(ItemStack stack, ListTag program) {
        if (program.isEmpty()) {
            stack.remove(RelayDataComponents.SPELL_PROGRAM);
            return;
        }

        CompoundTag programTag = new CompoundTag();
        programTag.put("program", program);
        stack.set(RelayDataComponents.SPELL_PROGRAM, programTag);
    }

    @Override
    public boolean hasProgram(ItemStack stack) {
        return stack.has(RelayDataComponents.SPELL_PROGRAM);
    }

    @Override
    public int getProgramSize(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return 0;
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        return listOpt.map(ListTag::size).orElse(0);
    }

    @Override
    public void clear(ItemStack stack) {
        stack.remove(RelayDataComponents.SPELL_PROGRAM);
    }
}
