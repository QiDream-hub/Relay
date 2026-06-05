package qdream.relay.mc;

import qdream.relay.engine.IData;
import qdream.relay.engine.IExecutable;
import qdream.relay.engine.StateMachine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * StateMachine NBT 序列化器
 */
public class StateMachineNbtSerializer {
    public static final StateMachineNbtSerializer INSTANCE = new StateMachineNbtSerializer();
    private final NbtSerializer iotaSerializer = NbtSerializer.INSTANCE;

    private StateMachineNbtSerializer() {}

    public CompoundTag serialize(StateMachine machine) {
        CompoundTag tag = new CompoundTag();

        ListTag programList = new ListTag();
        for (IExecutable iota : machine.getProgramStackSnapshot()) {
            if (iota instanceof McIota) {
                programList.add(iotaSerializer.serialize((McIota) iota));
            }
        }
        tag.put("programStack", programList);

        ListTag dataList = new ListTag();
        for (var data : machine.getDataStackSnapshot()) {
            if (data instanceof McIota) {
                dataList.add(iotaSerializer.serialize((McIota) data));
            }
        }
        tag.put("dataStack", dataList);

        tag.putBoolean("hasWorldInteractor", machine.hasWorldInteractor());
        tag.putInt("maxStackSize", machine.getMaxStackSize());

        return tag;
    }

    public void deserialize(StateMachine machine, CompoundTag tag) {
        ListTag programList = tag.getList("programStack").orElse(new ListTag());
        List<IExecutable> programStack = new ArrayList<>();
        for (Tag element : programList) {
            programStack.add(iotaSerializer.deserialize((CompoundTag) element));
        }
        // 反转后加载，保证执行顺序
        java.util.Collections.reverse(programStack);
        machine.loadProgram(programStack);

        ListTag dataList = tag.getList("dataStack").orElse(new ListTag());
        List<IData> dataStack = new ArrayList<>();
        for (Tag element : dataList) {
            dataStack.add(iotaSerializer.deserialize((CompoundTag) element));
        }
        // 数据栈需要反转后依次压入
        java.util.Collections.reverse(dataStack);
        for (var data : dataStack) {
            machine.pushData(data);
        }

        machine.setHasWorldInteractor(tag.getBoolean("hasWorldInteractor").orElse(false));
        machine.setMaxStackSize(tag.getInt("maxStackSize").orElse(1024));
    }
}
