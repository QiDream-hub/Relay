package qdream.relay.mc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import qdream.relay.engine.Executable;

/**
 * 操作注册表
 * 注册和管理所有可用的操作
 */
public class OperationRegistry {
    private static final Map<String, Executable> OPERATIONS = new HashMap<>();

    private OperationRegistry() {}

    /**
     * 注册操作
     */
    public static void register(String id, Executable operation) {
        OPERATIONS.put(id, operation);
    }

    /**
     * 获取操作
     */
    public static Optional<Executable> get(String id) {
        return Optional.ofNullable(OPERATIONS.get(id));
    }

    /**
     * 检查操作是否存在
     */
    public static boolean contains(String id) {
        return OPERATIONS.containsKey(id);
    }

    /**
     * 获取所有操作 ID
     */
    public static Set<String> getAllIds() {
        return Set.copyOf(OPERATIONS.keySet());
    }

    /**
     * 清除所有注册（用于测试）
     */
    public static void clear() {
        OPERATIONS.clear();
    }
}
