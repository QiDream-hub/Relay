package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 创建列表：数据栈大小不匹配
 * <p>翻译键：{@code error.list_create_size_mismatch}</p>
 */
public class ListCreateSizeMismatchError extends StackException {

    public ListCreateSizeMismatchError(StateMachine executor, int expectedSize, int actualSize) {
        super(executor, "error.list_create_size_mismatch", expectedSize, actualSize);
    }
}
