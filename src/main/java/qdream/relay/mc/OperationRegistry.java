package qdream.relay.mc;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Spell;

/**
 * 操作注册表
 * 注册和管理所有可用的操作和数据类型
 */
public class OperationRegistry {
    /**
     * 操作实例注册表（用于执行）
     * 存储单例操作实例，通过 ID 获取
     */
    private static final Map<String, Executable> OPERATIONS = new HashMap<>();

    /**
     * 数据类型注册表（用于反序列化）
     * 存储 Data 类的工厂方法，通过 ID 创建新实例
     */
    private static final Map<String, DataFactory> DATA_FACTORIES = new HashMap<>();

    private OperationRegistry() {}

    /**
     * 注册操作（单例）
     * @param id 操作 ID，如 "relay:add"
     * @param operation 操作实例
     */
    public static void register(String id, Executable operation) {
        OPERATIONS.put(id, operation);
    }

    /**
     * 注册数据类型
     * @param id 数据类型 ID，如 "relay:number"
     * @param factory 工厂方法，用于创建新实例
     */
    public static void registerData(String id, DataFactory factory) {
        DATA_FACTORIES.put(id, factory);
    }

    /**
     * 获取操作
     */
    public static Optional<Executable> get(String id) {
        return Optional.ofNullable(OPERATIONS.get(id));
    }

    /**
     * 创建数据类型实例
     * @param id 数据类型 ID
     * @return 新的 Data 实例，或空 Optional
     */
    public static Optional<Data> createData(String id) {
        DataFactory factory = DATA_FACTORIES.get(id);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.create());
    }

    /**
     * 通过对象自身的序列化方法序列化 Executable 为 NBT
     * @param exec 要序列化的 Executable
     * @return 序列化后的 CompoundTag，或空 Optional（如果类型不支持）
     */
    public static Optional<CompoundTag> serializeToNbt(Executable exec) {
        String id = ((qdream.relay.mc.base.Operation) exec).getId();
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);

        if (exec instanceof Data data) {
            data.toNbt(tag);
            return Optional.of(tag);
        } else if (exec instanceof Spell spell) {
            spell.toNbt(tag);
            return Optional.of(tag);
        } else {
            // 其他 Executable 类型，只保存 id
            return Optional.of(tag);
        }
    }

    /**
     * 通过注册表从 NBT 反序列化 Executable
     * @param tag NBT 标签
     * @return 反序列化后的 Executable，或空 Optional
     */
    public static Optional<Executable> deserializeFromNbt(CompoundTag tag) {
        String id = tag.getString("id").orElse("");
        
        // 尝试作为数据类型反序列化
        Optional<Data> dataOpt = createData(id);
        if (dataOpt.isPresent()) {
            return Optional.of(dataOpt.get().fromNbt(tag));
        }
        
        // 尝试作为操作获取（操作是单例，不需要反序列化）
        Optional<Executable> opOpt = get(id);
        if (opOpt.isPresent()) {
            return opOpt;
        }
        
        // 未知类型，返回空
        return Optional.empty();
    }

    /**
     * 检查操作是否存在
     */
    public static boolean contains(String id) {
        return OPERATIONS.containsKey(id);
    }

    /**
     * 检查数据类型是否已注册
     */
    public static boolean containsData(String id) {
        return DATA_FACTORIES.containsKey(id);
    }

    /**
     * 获取所有操作 ID
     */
    public static Set<String> getAllOperationIds() {
        return Set.copyOf(OPERATIONS.keySet());
    }

    /**
     * 获取所有数据类型 ID
     */
    public static Set<String> getAllDataIds() {
        return Set.copyOf(DATA_FACTORIES.keySet());
    }

    /**
     * 清除所有注册（用于测试）
     */
    public static void clear() {
        OPERATIONS.clear();
        DATA_FACTORIES.clear();
    }

    /**
     * 通过对象自身的序列化方法序列化 Executable 为 JSON
     * @param exec 要序列化的 Executable
     * @return 序列化后的 JsonObject，或空 Optional（如果类型不支持）
     */
    public static Optional<JsonObject> serializeToJson(Executable exec) {
        String id = ((qdream.relay.mc.base.Operation) exec).getId();
        JsonObject json = new JsonObject();
        json.addProperty("id", id);

        if (exec instanceof Data data) {
            data.toJson(json);
            return Optional.of(json);
        } else if (exec instanceof Spell spell) {
            spell.toJson(json);
            return Optional.of(json);
        } else {
            // 其他 Executable 类型，只保存 id
            return Optional.of(json);
        }
    }

    /**
     * 通过注册表从 JSON 反序列化 Executable
     * @param json JSON 对象
     * @return 反序列化后的 Executable，或空 Optional
     */
    public static Optional<Executable> deserializeFromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "";

        // 尝试作为数据类型反序列化
        Optional<Data> dataOpt = createData(id);
        if (dataOpt.isPresent()) {
            return Optional.of(dataOpt.get().fromJson(json));
        }

        // 尝试作为操作获取（操作是单例，不需要反序列化）
        Optional<Executable> opOpt = get(id);
        if (opOpt.isPresent()) {
            return opOpt;
        }

        // 未知类型，返回空
        return Optional.empty();
    }

    /**
     * 数据类型工厂接口
     */
    @FunctionalInterface
    public interface DataFactory {
        Data create();
    }
}
