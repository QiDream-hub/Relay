package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据字段描述符
 * 用于描述数据类型序列化/反序列化时的字段信息
 * 
 * <h2>设计原则</h2>
 * <ul>
 *   <li><strong>name</strong>: 字段名称，与 NBT/JSON 中的字段名对应（如 "x", "y", "world", "uuid"）</li>
 *   <li><strong>types</strong>: 该字段可接受的值类型（如 "Number", "String"）</li>
 * </ul>
 * 
 * <h2>与 ParameterDescriptor 的区别</h2>
 * <ul>
 *   <li>{@link ParameterDescriptor} 用于操作签名，描述从哪个栈消费/向哪个栈生产</li>
 *   <li>{@link DataFieldDescriptor} 用于数据类型签名，描述构建实例需要的字段</li>
 * </ul>
 * 
 * <h2>示例</h2>
 * <pre>
 * // VectorType 的字段描述
 * new DataFieldDescriptor("x", "Number")
 * new DataFieldDescriptor("y", "Number")
 * new DataFieldDescriptor("z", "Number")
 * 
 * // BlockEntityType 的字段描述
 * new DataFieldDescriptor("world", "String")
 * new DataFieldDescriptor("x", "Number")
 * new DataFieldDescriptor("y", "Number")
 * new DataFieldDescriptor("z", "Number")
 * </pre>
 */
public class DataFieldDescriptor {
    private final String name;
    private final List<String> types;

    /**
     * @param name 字段名称（如 "x", "y", "world"）
     * @param types 该字段可接受的类型列表（如 ["Number"], ["String"]）
     */
    public DataFieldDescriptor(String name, String... types) {
        this.name = name;
        this.types = Collections.unmodifiableList(new ArrayList<>(List.of(types)));
    }

    /**
     * @param name 字段名称
     * @param types 该字段可接受的类型列表
     */
    public DataFieldDescriptor(String name, List<String> types) {
        this.name = name;
        this.types = Collections.unmodifiableList(new ArrayList<>(types));
    }

    /**
     * 获取字段名称
     * @return 字段名（如 "x", "uuid", "world"）
     */
    public String getName() {
        return name;
    }

    /**
     * 获取该字段可接受的类型列表
     * @return 类型列表（如 ["Number"], ["String"]）
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * 获取第一个类型（便捷方法）
     * @return 第一个类型，如果没有则返回 null
     */
    public String getType() {
        return types.isEmpty() ? null : types.get(0);
    }

    @Override
    public String toString() {
        if (types.isEmpty()) {
            return name;
        }
        return name + ": " + String.join(" | ", types);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataFieldDescriptor that)) return false;
        return name.equals(that.name) && types.equals(that.types);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + types.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private final List<String> types = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.types.add(type);
            return this;
        }

        public Builder types(String... types) {
            Collections.addAll(this.types, types);
            return this;
        }

        public Builder types(List<String> types) {
            this.types.addAll(types);
            return this;
        }

        public DataFieldDescriptor build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("字段名称不能为空");
            }
            return new DataFieldDescriptor(name, types);
        }
    }
}
