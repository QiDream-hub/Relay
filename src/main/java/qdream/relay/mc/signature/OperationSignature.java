package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 操作类型签名
 * 用于描述操作的输入和输出类型
 * 使用字符串表示类型，使 engine 包不依赖具体类型定义
 */
public class OperationSignature implements Signature<List<String>, List<String>> {
    private final List<List<String>> inputs;
    private final List<List<String>> outputs;

    public OperationSignature(List<List<String>> inputs, List<List<String>> outputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<List<String>> getInputs() {
        return inputs;
    }

    public List<List<String>> getOutputs() {
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

    public String inputAt(int index, int inputIndex) {
        return inputs.get(index).get(inputIndex);
    }

    public String outputAt(int index, int outputIndex) {
        return outputs.get(index).get(outputIndex);
    }

    public static class Builder {
        private final List<List<String>> inputs = new ArrayList<>();
        private final List<List<String>> outputs = new ArrayList<>();

        public Builder input(List<String> type) {
            inputs.add(type);
            return this;
        }

        public Builder input(String type) {
            inputs.add(List.of(type));
            return this;
        }

        public Builder output(List<String> type) {
            outputs.add(type);
            return this;
        }

        public Builder output(String type) {
            outputs.add(List.of(type));
            return this;
        }

        public OperationSignature build() {
            return new OperationSignature(inputs, outputs);
        }
    }
}
