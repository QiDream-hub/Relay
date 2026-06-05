package qdream.relay.mc;

import qdream.relay.engine.Iota;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.serialization.StateMachineSerializer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * StateMachine NBT 序列化器
 */
public class StateMachineNbtSerializer implements StateMachineSerializer<CompoundTag> {
    public static final StateMachineNbtSerializer INSTANCE = new StateMachineNbtSerializer();
    private final NbtSerializer iotaSerializer = NbtSerializer.INSTANCE;

    private StateMachineNbtSerializer() {}

    @Override
    public CompoundTag serialize(StateMachine machine) {
        CompoundTag tag = new CompoundTag();

        ListTag programList = new ListTag();
        for (Iota iota : machine.getProgramStackSnapshot()) {
            programList.add(iotaSerializer.serialize(iota));
        }
        tag.put("programStack", programList);

        ListTag dataList = new ListTag();
        for (Iota iota : machine.getDataStackSnapshot()) {
            dataList.add(iotaSerializer.serialize(iota));
        }
        tag.put("dataStack", dataList);

        tag.putBoolean("hasWorldInteractor", machine.hasWorldInteractor());
        tag.putInt("maxStackSize", machine.getMaxStackSize());

        return tag;
    }

    @Override
    public void deserialize(StateMachine machine, CompoundTag tag) {
        ListTag programList = tag.getList("programStack").orElse(new ListTag());
        List<Iota> programStack = new ArrayList<>();
        for (Tag element : programList) {
            programStack.add(iotaSerializer.deserialize((CompoundTag) element));
        }
        // 反转后加载，保证执行顺序
        java.util.Collections.reverse(programStack);
        machine.loadProgram(programStack);

        ListTag dataList = tag.getList("dataStack").orElse(new ListTag());
        List<Iota> dataStack = new ArrayList<>();
        for (Tag element : dataList) {
            dataStack.add(iotaSerializer.deserialize((CompoundTag) element));
        }
        // 数据栈需要反转后依次压入
        java.util.Collections.reverse(dataStack);
        for (Iota iota : dataStack) {
            machine.pushData(iota);
        }

        machine.setHasWorldInteractor(tag.getBoolean("hasWorldInteractor").orElse(false));
        machine.setMaxStackSize(tag.getInt("maxStackSize").orElse(1024));
    }
}
