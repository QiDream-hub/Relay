package qdream.relay.mc;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.Relay;

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

    private StateMachineNbtSerializer() {}

    public CompoundTag serialize(StateMachine machine) {
        CompoundTag tag = new CompoundTag();

        ListTag programList = new ListTag();
        for (Executable iota : machine.getProgramStackSnapshot()) {
            if (iota != null) {
                CompoundTag itemTag = new CompoundTag();
                ((Operation) iota).toNbt(itemTag);
                programList.add(itemTag);
            } else {
                Relay.LOGGER.warn("Skipping null executable in programStack");
            }
        }
        tag.put("programStack", programList);

        ListTag dataList = new ListTag();
        for (Executable data : machine.getDataStackSnapshot()) {
            if (data != null) {
                CompoundTag itemTag = new CompoundTag();
                ((Operation) data).toNbt(itemTag);
                dataList.add(itemTag);
            } else {
                Relay.LOGGER.warn("Skipping null executable in dataStack");
            }
        }
        tag.put("dataStack", dataList);

        tag.putInt("maxStackSize", machine.getMaxStackSize());

        return tag;
    }

    public void deserialize(StateMachine machine, CompoundTag tag) {
        // 直接恢复程序栈 - 不调用 loadProgram()，因为那会清空现有栈
        ListTag programList = tag.getList("programStack").orElse(new ListTag());
        List<Executable> programStack = new ArrayList<>();
        for (Tag element : programList) {
            if (element instanceof CompoundTag compoundTag) {
                String id = compoundTag.getString("id").orElse("");
                OperationRegistry.getEntry(id).ifPresent(entry -> {
                    Operation instance = (Operation) entry.create();
                    programStack.add(instance.fromNbt(compoundTag));
                });
            }
        }
        // 反转后压入，保证执行顺序
        java.util.Collections.reverse(programStack);
        for (Executable exe : programStack) {
            machine.pushProgram(exe);
        }

        // 恢复数据栈
        ListTag dataList = tag.getList("dataStack").orElse(new ListTag());
        List<Executable> dataStack = new ArrayList<>();
        for (Tag element : dataList) {
            if (element instanceof CompoundTag compoundTag) {
                String id = compoundTag.getString("id").orElse("");
                OperationRegistry.getEntry(id).ifPresent(entry -> {
                    Operation instance = (Operation) entry.create();
                    dataStack.add(instance.fromNbt(compoundTag));
                });
            }
        }
        // 数据栈需要反转后依次压入
        java.util.Collections.reverse(dataStack);
        for (Executable data : dataStack) {
            machine.pushData(data);
        }

        machine.setMaxStackSize(tag.getInt("maxStackSize").orElse(1024));
    }
}
