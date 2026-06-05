package qdream.relay.engine.serialization;

import qdream.relay.engine.StateMachine;

/**
 * StateMachine 序列化接口
 * @param <T> 序列化目标类型
 */
public interface StateMachineSerializer<T> {
    T serialize(StateMachine machine);
    void deserialize(StateMachine machine, T data);
}
