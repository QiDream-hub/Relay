package qdream.relay.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 操作注册表
 * 注册和管理所有可用的操作
 */
public class OperationRegistry {
    private static final Map<String, OperationEntry> OPERATIONS = new HashMap<>();

    private OperationRegistry() {}

    /**
     * 链式注册器
     */
    public static ChainBuilder register(String id, StackOperation operation) {
        return new ChainBuilder(id, operation);
    }

    /**
     * 获取操作
     */
    public static Optional<StackOperation> get(String id) {
        return Optional.ofNullable(OPERATIONS.get(id)).map(OperationEntry::getOperation);
    }

    /**
     * 获取操作条目（包含签名等元数据）
     */
    public static Optional<OperationEntry> getEntry(String id) {
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

    /**
     * 操作条目
     */
    public static class OperationEntry {
        private final StackOperation operation;
        private OperationSignature signature;
        private int cost;
        private boolean requiresWorldInteractor;

        public OperationEntry(StackOperation operation) {
            this(operation, null, 1, false);
        }

        public OperationEntry(StackOperation operation, OperationSignature signature, int cost, boolean requiresWorldInteractor) {
            this.operation = operation;
            this.signature = signature != null ? signature : OperationSignature.builder().build();
            this.cost = cost;
            this.requiresWorldInteractor = requiresWorldInteractor;
        }

        public StackOperation getOperation() {
            return operation;
        }

        public OperationSignature getSignature() {
            return signature;
        }

        public int getCost() {
            return cost;
        }

        public boolean requiresWorldInteractor() {
            return requiresWorldInteractor;
        }

        void setSignature(OperationSignature signature) {
            this.signature = signature;
        }

        void setCost(int cost) {
            this.cost = cost;
        }

        void setRequiresWorldInteractor(boolean requires) {
            this.requiresWorldInteractor = requires;
        }
    }

    /**
     * 链式注册构建器
     */
    public static class ChainBuilder {
        private final String id;
        private final StackOperation operation;
        private OperationSignature signature;
        private int cost = 1;
        private boolean requiresWorldInteractor = false;

        public ChainBuilder(String id, StackOperation operation) {
            this.id = id;
            this.operation = operation;
        }

        public ChainBuilder signature(OperationSignature sig) {
            this.signature = sig;
            return this;
        }

        public ChainBuilder cost(int cost) {
            this.cost = cost;
            return this;
        }

        public ChainBuilder requiresWorldInteractor(boolean requires) {
            this.requiresWorldInteractor = requires;
            return this;
        }

        public void register() {
            OPERATIONS.put(id, new OperationEntry(operation, signature, cost, requiresWorldInteractor));
        }
    }
}
