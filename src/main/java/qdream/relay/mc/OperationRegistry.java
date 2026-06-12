package qdream.relay.mc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import qdream.relay.engine.Executable;

/**
 * 统一注册表
 * 注册和管理所有可用的操作和数据类型
 * <p>
 * 操作和数据类型共用同一张注册表，通过 {@link Entry} 接口统一管理。
 * 序列化/反序列化通过各类型的 toJson/fromJson/toNbt/fromNbt 方法完成。
 */
public class OperationRegistry {

    /**
     * 统一注册条目接口
     */
    public interface Entry {
        /** 创建实例（操作返回单例，数据类型返回默认值实例） */
        Executable create();

        /** 是否为数据类型 */
        boolean isDataType();
    }

    /**
     * 操作条目（单例）
     */
    public static class OpEntry implements Entry {
        private final Executable singleton;

        public OpEntry(Executable singleton) {
            this.singleton = singleton;
        }

        @Override
        public Executable create() { return singleton; }

        @Override
        public boolean isDataType() { return false; }
    }

    /**
     * 数据类型条目（工厂）
     */
    public static class DataEntry implements Entry {
        private final java.util.function.Supplier<Executable> factory;

        public DataEntry(java.util.function.Supplier<Executable> factory) {
            this.factory = factory;
        }

        @Override
        public Executable create() { return factory.get(); }

        @Override
        public boolean isDataType() { return true; }
    }

    /**
     * 统一注册表
     */
    private static final Map<String, Entry> REGISTRY = new HashMap<>();

    private OperationRegistry() {}

    // ========== 注册 ==========

    /**
     * 注册条目
     * @param id 注册 ID，如 "relay:add" 或 "relay:number"
     * @param entry 注册条目
     */
    public static void register(String id, Entry entry) {
        REGISTRY.put(id, entry);
    }

    // ========== 查找 ==========

    /**
     * 获取注册条目
     */
    public static Optional<Entry> getEntry(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    /**
     * 获取 Executable 实例（使用 create）
     */
    public static Optional<Executable> get(String id) {
        Entry entry = REGISTRY.get(id);
        return entry != null ? Optional.of(entry.create()) : Optional.empty();
    }

    /**
     * 检查条目是否存在
     */
    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }

    // ========== 查询 ==========

    /**
     * 获取所有操作 ID
     */
    public static Set<String> getAllOperationIds() {
        return REGISTRY.entrySet().stream()
            .filter(e -> !e.getValue().isDataType())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * 获取所有数据类型 ID
     */
    public static Set<String> getAllDataIds() {
        return REGISTRY.entrySet().stream()
            .filter(e -> e.getValue().isDataType())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * 清除所有注册（用于测试）
     */
    public static void clear() {
        REGISTRY.clear();
    }
}
