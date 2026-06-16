package qdream.relay.mc.signature;

import java.util.ArrayList;
import java.util.List;

public class SignatureName {
    private String name;
    private List<String> type;

    public SignatureName(List<String> type, String name) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public List<String> getType() {
        return type;
    }
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private List<String> type = new ArrayList<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(String type) {
            this.type.add(type);
            return this;
        }

        public SignatureName build() {
            return new SignatureName(this.type, this.name);
        }
    }
}