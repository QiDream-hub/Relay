package qdream.relay.mc.errors.entity;

import qdream.relay.engine.StateMachine;

/**
 * 实体生成失败
 * <p>翻译键：{@code error.entity_spawn_failed}</p>
 */
public class EntitySpawnFailedError extends EntityException {

    public EntitySpawnFailedError(StateMachine executor) {
        super(executor, "error.entity_spawn_failed");
    }
}
