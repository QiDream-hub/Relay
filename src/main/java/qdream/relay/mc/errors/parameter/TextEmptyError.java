package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 文本不能为空
 * <p>翻译键：{@code error.text_empty}</p>
 */
public class TextEmptyError extends ParameterException {

    public TextEmptyError(StateMachine executor) {
        super(executor, "error.text_empty");
    }
}
