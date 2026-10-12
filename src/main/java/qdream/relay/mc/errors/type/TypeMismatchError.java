package qdream.relay.mc.errors.type;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 类型不匹配
 * <p>翻译键：{@code error.type_mismatch}</p>
 */
public class TypeMismatchError extends TypeException {

    public TypeMismatchError(StateMachine executor, Object expected, Object actual) {
        super(executor, "error.type_mismatch", expected, actual);
    }

    public TypeMismatchError(StateMachine executor, Component prefix, Object expected, Object actual) {
        super(executor, prefix, "error.type_mismatch", expected, actual);
    }
}
