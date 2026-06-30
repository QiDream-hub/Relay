package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据类型签名
 * 用于描述数据类型的序列化/反序列化接口
 * 
 * <h2>设计原则</h2>
 * <ul>
 *   <li><strong>output</strong>: 描述该类型执行后向数据栈压入的类型（即类型自身的 ID）</li>
 *   <li><strong>input</strong>: 描述构建该类型实例时需要的字段，使用 {@link DataFieldDescriptor}</li>
 * </ul>
 * 
 * <h2>与 OperationSignature 的区别</h2>
 * <ul>
 *   <li>{@link OperationSignature} 描述操作对程序栈和数据栈的影响（消费/生产）</li>
 *   <li>{@link DataSignature} 描述数据类型的构建字段（NBT/JSON 字段名）</li>
 * </ul>
 * 
 * <h2>示例</h2>
 * <pre>
 * // BlockEntityType 的签名
 * DataSignature.builder()
 *     .output("relay:block_entity")
 *     .field("world", "String")    // 对应 NBT 中的 "world" 字段
 *     .field("x", "Number")        // 对应 NBT 中的 "x" 字段
 *     .field("y", "Number")        // 对应 NBT 中的 "y" 字段
 *     .field("z", "Number")        // 对应 NBT 中的 "z" 字段
 *     .build()
 * 
 * // NumberType 的签名
 * DataSignature.builder()
 *     .output("relay:number")
 *     .field("number", "Number")   // 对应 NBT 中的 "number" 字段
 *     .build()
 * 
 * // VectorType 的签名
 * DataSignature.builder()
 *     .output("relay:vector")
 *     .field("x", "Number")
 *     .field("y", "Number")
 *     .field("z", "Number")
 *     .build()
 * </pre>
 */
public class DataSignature implements Signature<DataFieldDescriptor, String> {
    private final List<DataFieldDescriptor> inputs;
    private final List<String> outputs;

    public DataSignature(List<DataFieldDescriptor> inputs, List<String> outputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<DataFieldDescriptor> getInputs() {
        return inputs;
    }

    @Override
    public List<String> getOutputs() {
        return outputs;
    }

    @Override
    public int inputCount() {
        return inputs.size();
    }

    @Override
    public int outputCount() {
        return outputs.size();
    }

    /**
     * 获取指定索引的输入字段
     * @param index 字段索引
     * @return 字段描述符
     */
    public DataFieldDescriptor inputAt(int index) {
        return inputs.get(index);
    }

    /**
     * 获取指定索引的输出类型
     * @param index 类型索引
     * @return 类型 ID
     */
    public String outputAt(int index) {
        return outputs.get(index);
    }

    public static class Builder {
        private final List<DataFieldDescriptor> inputs = new ArrayList<>();
        private final List<String> outputs = new ArrayList<>();

        /**
         * 添加输入字段
         * @param name 字段名称，应与 NBT/JSON 中的字段名对应
         * @param types 字段可接受的类型
         */
        public Builder field(String name, String... types) {
            inputs.add(new DataFieldDescriptor(name, types));
            return this;
        }

        /**
         * 添加输入字段（使用构建器）
         * @param builder 字段构建器
         */
        public Builder field(DataFieldDescriptor.Builder builder) {
            inputs.add(builder.build());
            return this;
        }

        /**
         * 添加输出类型
         * @param typeId 类型 ID，即执行后压入数据栈的类型
         */
        public Builder output(String typeId) {
            outputs.add(typeId);
            return this;
        }

        public DataSignature build() {
            return new DataSignature(inputs, outputs);
        }
    }

}
