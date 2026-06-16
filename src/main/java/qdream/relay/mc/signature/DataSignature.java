package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据类型签名
 * 用于描述数据构建时的输入和输出类型
 * 使用字符串表示类型，使 engine 包不依赖具体类型定义
 */
public class DataSignature implements Signature<SignatureName, List<String>> {
    private final List<SignatureName> inputs;
    private final List<List<String>> outputs;

    public DataSignature(List<SignatureName> inputs, List<List<String>> outputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<SignatureName> getInputs() {
        return inputs;
    }

    @Override
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

    public String outputAt(int index, int outputIndex) {
        return outputs.get(index).get(outputIndex);
    }

    public static class Builder {
        private final List<SignatureName> inputs = new ArrayList<>();
        private final List<List<String>> outputs = new ArrayList<>();

        public Builder input(SignatureName type) {
            inputs.add(type);
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

        public DataSignature build() {
            return new DataSignature(inputs, outputs);
        }
    }

}
