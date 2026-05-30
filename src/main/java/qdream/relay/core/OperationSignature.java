package qdream.relay.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 操作类型签名
 * 用于描述操作的输入和输出类型
 */
public class OperationSignature {
    private final List<IotaType> inputs;
    private final List<IotaType> outputs;

    public OperationSignature(List<IotaType> inputs, List<IotaType> outputs) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<IotaType> getInputs() {
        return inputs;
    }

    public List<IotaType> getOutputs() {
        return outputs;
    }

    public int inputCount() {
        return inputs.size();
    }

    public int outputCount() {
        return outputs.size();
    }

    public IotaType inputAt(int index) {
        return inputs.get(index);
    }

    public IotaType outputAt(int index) {
        return outputs.get(index);
    }

    public static class Builder {
        private final List<IotaType> inputs = new ArrayList<>();
        private final List<IotaType> outputs = new ArrayList<>();

        public Builder input(IotaType type) {
            inputs.add(type);
            return this;
        }

        public Builder output(IotaType type) {
            outputs.add(type);
            return this;
        }

        public OperationSignature build() {
            return new OperationSignature(inputs, outputs);
        }
    }
}
