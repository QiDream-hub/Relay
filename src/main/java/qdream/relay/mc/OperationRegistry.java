package qdream.relay.mc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Data;

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
     * @param factory 工厂方法，用于从 JSON 创建 Data 实例
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
     * 数据类型工厂接口
     */
    @FunctionalInterface
    public interface DataFactory {
        Data create();
    }
}
