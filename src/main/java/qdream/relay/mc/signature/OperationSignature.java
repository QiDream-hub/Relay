package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 操作签名
 * 描述操作对程序栈和数据栈的影响
 */
public class OperationSignature implements Signature<ParameterDescriptor, ParameterDescriptor> {
    private final List<ParameterDescriptor> consumesFromData;
    private final List<ParameterDescriptor> consumesFromProgram;
    private final List<ParameterDescriptor> producesToData;
    private final List<ParameterDescriptor> producesToProgram;

    public OperationSignature(
            List<ParameterDescriptor> consumesFromData,
            List<ParameterDescriptor> consumesFromProgram,
            List<ParameterDescriptor> producesToData,
            List<ParameterDescriptor> producesToProgram) {
        this.consumesFromData = Collections.unmodifiableList(new ArrayList<>(consumesFromData));
        this.consumesFromProgram = Collections.unmodifiableList(new ArrayList<>(consumesFromProgram));
        this.producesToData = Collections.unmodifiableList(new ArrayList<>(producesToData));
        this.producesToProgram = Collections.unmodifiableList(new ArrayList<>(producesToProgram));
    }

    @Override
    public List<ParameterDescriptor> getInputs() {
        // 对于操作签名，输入包括从数据栈和程序栈消费的参数
        List<ParameterDescriptor> allInputs = new ArrayList<>();
        allInputs.addAll(consumesFromData);
        allInputs.addAll(consumesFromProgram);
        return Collections.unmodifiableList(allInputs);
    }

    @Override
    public List<ParameterDescriptor> getOutputs() {
        // 对于操作签名，输出包括向数据栈和程序栈生产的参数
        List<ParameterDescriptor> allOutputs = new ArrayList<>();
        allOutputs.addAll(producesToData);
        allOutputs.addAll(producesToProgram);
        return Collections.unmodifiableList(allOutputs);
    }

    @Override
    public int inputCount() {
        return consumesFromData.size() + consumesFromProgram.size();
    }

    @Override
    public int outputCount() {
        return producesToData.size() + producesToProgram.size();
    }

    public List<ParameterDescriptor> getConsumesFromData() {
        return consumesFromData;
    }

    public List<ParameterDescriptor> getConsumesFromProgram() {
        return consumesFromProgram;
    }

    public List<ParameterDescriptor> getProducesToData() {
        return producesToData;
    }

    public List<ParameterDescriptor> getProducesToProgram() {
        return producesToProgram;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<ParameterDescriptor> consumesFromData = new ArrayList<>();
        private final List<ParameterDescriptor> consumesFromProgram = new ArrayList<>();
        private final List<ParameterDescriptor> producesToData = new ArrayList<>();
        private final List<ParameterDescriptor> producesToProgram = new ArrayList<>();

        /**
         * 添加从数据栈消费的参数
         */
        public Builder consumesFromData(ParameterDescriptor descriptor) {
            consumesFromData.add(descriptor);
            return this;
        }

        /**
         * 添加从数据栈消费的参数
         */
        public Builder consumesFromData(String... types) {
            consumesFromData.add(ParameterDescriptor.builder().types(types).build());
            return this;
        }

        /**
         * 添加从程序栈消费的参数
         */
        public Builder consumesFromProgram(ParameterDescriptor descriptor) {
            consumesFromProgram.add(descriptor);
            return this;
        }

        /**
         * 添加从程序栈消费的参数
         */
        public Builder consumesFromProgram(String... types) {
            consumesFromProgram.add(ParameterDescriptor.builder()
                    .source(ParameterSource.PROGRAM_STACK)
                    .types(types)
                    .build());
            return this;
        }

        /**
         * 添加向数据栈生产的参数
         */
        public Builder producesToData(ParameterDescriptor descriptor) {
            producesToData.add(descriptor);
            return this;
        }

        /**
         * 添加向数据栈生产的参数
         */
        public Builder producesToData(String... types) {
            producesToData.add(ParameterDescriptor.builder().types(types).build());
            return this;
        }

        /**
         * 添加向程序栈生产的参数
         */
        public Builder producesToProgram(ParameterDescriptor descriptor) {
            producesToProgram.add(descriptor);
            return this;
        }

        /**
         * 添加向程序栈生产的参数
         */
        public Builder producesToProgram(String... types) {
            producesToProgram.add(ParameterDescriptor.builder()
                    .source(ParameterSource.PROGRAM_STACK)
                    .types(types)
                    .build());
            return this;
        }

        public OperationSignature build() {
            return new OperationSignature(
                    consumesFromData,
                    consumesFromProgram,
                    producesToData,
                    producesToProgram);
        }
    }
}
