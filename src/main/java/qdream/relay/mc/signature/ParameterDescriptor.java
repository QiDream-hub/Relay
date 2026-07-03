package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 参数描述
 * 描述操作的一个输入或输出参数的类型和来源信息
 */
public class ParameterDescriptor {
    private final ParameterSource source;
    private final List<String> types;
    private final String name;

    public ParameterDescriptor(ParameterSource source, String name, List<String> types) {
        this.source = source;
        this.name = name;
        this.types = Collections.unmodifiableList(new ArrayList<>(types));
    }

    public ParameterSource getSource() {
        return source;
    }

    public List<String> getTypes() {
        return types;
    }

    public String getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ParameterSource source = ParameterSource.DATA_STACK;
        private String name = "";
        private final List<String> types = new ArrayList<>();

        public Builder source(ParameterSource source) {
            this.source = source;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
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

        public ParameterDescriptor build() {
            return new ParameterDescriptor(source, name, types);
        }
    }
}
