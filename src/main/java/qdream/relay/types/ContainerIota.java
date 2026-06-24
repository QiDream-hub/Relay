package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

/**
 * 外壳容器数据类型
 * 用于在程序中存储对 ShellContainer 的引用
 * 
 * 注意：这是一个临时引用，不会被序列化到磁盘
 * 只在程序运行时有效
 */
public class ContainerIota extends Data {
    private final ShellContainer container;

    public ContainerIota(ShellContainer container) {
        super("relay:container", 0, DataSignature.builder()
                .output("relay:container")
                .build());
        this.container = container;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    /**
     * 获取容器引用
     */
    public ShellContainer getContainer() {
        return container;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        // 注意：ShellContainer 引用无法被序列化
        // 这是运行时临时数据
        CompoundTag valueTag = new CompoundTag();
        valueTag.putString("type", "runtime_only");
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        // 反序列化时返回 null 容器
        // 这表示该数据只在运行时有效
        return new ContainerIota(null);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject valueObject = new JsonObject();
        valueObject.addProperty("type", "runtime_only");
        json.add("value", valueObject);
    }

    @Override
    public Data fromJson(JsonObject json) {
        // JSON 反序列化时返回 null 容器
        return new ContainerIota(null);
    }
}
