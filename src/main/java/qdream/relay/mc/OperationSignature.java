package qdream.relay.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 操作类型签名
 * 用于描述操作的输入和输出类型
 * 使用字符串表示类型，使 engine 包不依赖具体类型定义
 */
public class OperationSignature {
    private final List<String> inputs;
    private final List<String> outputs;

    public OperationSignature(List<String> inputs, List<String> outputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getInputs() {
        return inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public int inputCount() {
        return inputs.size();
    }

    public int outputCount() {
        return outputs.size();
    }

    public String inputAt(int index) {
        return inputs.get(index);
    }

    public String outputAt(int index) {
        return outputs.get(index);
    }

    public static class Builder {
        private final List<String> inputs = new ArrayList<>();
        private final List<String> outputs = new ArrayList<>();

        public Builder input(String type) {
            inputs.add(type);
            return this;
        }

        public Builder output(String type) {
            outputs.add(type);
            return this;
        }

        public OperationSignature build() {
            return new OperationSignature(inputs, outputs);
        }
    }
}
