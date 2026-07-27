package qdream.relay.items;

import java.util.ArrayList;
import java.util.List;
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
import qdream.relay.engine.Executable;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.tools.StackTools;

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
        List<Executable> program = getProgram(stack);
        if (!program.isEmpty()) {
            // 最多显示 8 行，避免工具提示过长
            int maxLines = 8;
            int displayCount = Math.min(program.size(), maxLines);
            
            for (int i = 0; i < displayCount; i++) {
                Executable exec = program.get(i);
                // 使用工具类获取显示名称翻译键
                String nameKey = StackTools.getNameKey(exec);
                // 创建翻译组件（客户端渲染时会自动解析为本地化文本）
                // 不要调用 .getString() 检查 - 服务器端无法解析翻译键是正常的
                Component name = Component.translatable(nameKey);
                textConsumer.accept(Component.literal("  • ").withStyle(ChatFormatting.GRAY)
                        .append(name.copy().withStyle(ChatFormatting.WHITE)));
            }
            
            // 如果程序超过显示行数，显示省略提示
            if (program.size() > maxLines) {
                int remaining = program.size() - maxLines;
                textConsumer.accept(Component.translatable("item.relay.spell_disk.more", remaining)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public List<Executable> getProgram(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return List.of();
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        if (listOpt.isEmpty()) {
            return List.of();
        }
        ListTag listTag = listOpt.get();
        try {
            return ProgramCompiler.fromNbt(listTag);
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    public void setProgram(ItemStack stack, List<Executable> program) {
        if (program.isEmpty()) {
            stack.remove(RelayDataComponents.SPELL_PROGRAM);
            return;
        }

        CompoundTag programTag = new CompoundTag();
        try {
            ListTag listTag = ProgramCompiler.toNbt(program);
            programTag.put("program", listTag);
            stack.set(RelayDataComponents.SPELL_PROGRAM, programTag);
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveFromStateMachine(ItemStack stack, List<Executable> program) {
        // 反转回原始顺序（快照是栈顺序，需要转为列表顺序）
        List<Executable> reversed = new ArrayList<>(program);
        java.util.Collections.reverse(reversed);

        // 保存程序
        setProgram(stack, reversed);
    }

    @Override
    public void loadToStateMachine(ItemStack stack, StateMachine machine) {
        List<Executable> program = getProgram(stack);
        if (!program.isEmpty()) {
            machine.loadProgram(program);
        }
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

    @Override
    public String exportToJson(ItemStack stack) {
        List<Executable> program = getProgram(stack);
        return ProgramCompiler.toJsonString(program);
    }

    @Override
    public void importFromJson(ItemStack stack, String jsonStr) throws ProgramCompiler.CompilationException {
        List<Executable> program = ProgramCompiler.compileFromJson(jsonStr);
        setProgram(stack, program);
    }
}
