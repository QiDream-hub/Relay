package qdream.relay.items;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.function.Consumer;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.engine.Executable;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.tools.TextTools;

/**
 * 法术磁盘物品
 * 存储栈图程序（JSON 字符串）
 * 使用 26.1.2 DataComponent 系统存储 JSON 字符串
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

        try {
            // 显示程序内容，自动换行
            String programJson = getProgram(stack);
            if (programJson != null && !programJson.trim().isEmpty()) {
                List<Executable> program = ProgramCompiler.compileFromJson(programJson);
                if (!program.isEmpty()) {
                    // 最多显示 8 行，避免工具提示过长
                    int maxLines = 8;
                    int displayCount = Math.min(program.size(), maxLines);

                    for (int i = 0; i < displayCount; i++) {
                        textConsumer.accept(
                                Component.literal("  • " + TextTools.getName(program.get(i)))
                                        .withStyle(ChatFormatting.GRAY));
                    }

                    // 如果程序超过显示行数，显示省略提示
                    if (program.size() > maxLines) {
                        int remaining = program.size() - maxLines;
                        textConsumer.accept(Component.translatable("item.relay.spell_disk.more", remaining)
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
        } catch (CompilationException e) {
            textConsumer.accept(Component.translatable("item.relay.spell_disk.compilation_error", e.getMessage()));
            textConsumer.accept(Component.translatable(e.getMessage()).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public String getProgram(ItemStack stack) {
        return stack.get(RelayDataComponents.SPELL_PROGRAM_JSON);
    }

    @Override
    public void setProgram(ItemStack stack, String programJson) {
        if (programJson == null || programJson.trim().isEmpty()) {
            stack.remove(RelayDataComponents.SPELL_PROGRAM_JSON);
            return;
        }
        stack.set(RelayDataComponents.SPELL_PROGRAM_JSON, programJson);
    }

    @Override
    public boolean hasProgram(ItemStack stack) {
        return stack.has(RelayDataComponents.SPELL_PROGRAM_JSON);
    }

    @Override
    public int getProgramSize(ItemStack stack) {
        String programJson = getProgram(stack);
        if (programJson == null || programJson.trim().isEmpty()) {
            return 0;
        }
        try {
            List<Executable> program = ProgramCompiler.compileFromJson(programJson);
            return program.size();
        } catch (CompilationException e) {
            return 0;
        }
    }

    @Override
    public void clear(ItemStack stack) {
        stack.remove(RelayDataComponents.SPELL_PROGRAM_JSON);
    }
}
