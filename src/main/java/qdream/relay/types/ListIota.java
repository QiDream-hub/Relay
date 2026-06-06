package qdream.relay.types;

import java.util.List;
import java.util.ArrayList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.base.Data;

/**
 * 列表类型
 */
public class ListIota extends Data {
    private final List<Executable> value;

    public ListIota(List<Executable> value) {
        super("relay:list", 0);
        this.value = value != null ? new ArrayList<>(value) : new ArrayList<>();
    }

    public List<Executable> getValue() {
        return new ArrayList<>(value);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public void toNbt(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (Executable item : value) {
            OperationRegistry.serializeToNbt(item).ifPresent(listTag::add);
        }
        tag.put("value", listTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        var valueOpt = tag.getList("value");
        if (valueOpt.isPresent()) {
            ListTag listTag = valueOpt.get();
            List<Executable> list = new ArrayList<>();
            for (Tag element : listTag) {
                if (element instanceof CompoundTag compoundTag) {
                    OperationRegistry.deserializeFromNbt(compoundTag).ifPresent(list::add);
                }
            }
            return new ListIota(list);
        } else {
            return new ListIota(new ArrayList<>());
        }
    }
}
