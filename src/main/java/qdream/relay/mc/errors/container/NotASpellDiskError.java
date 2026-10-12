package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 目标物品不是法术磁盘
 * <p>翻译键：{@code error.not_a_spell_disk}</p>
 */
public class NotASpellDiskError extends ContainerException {

    public NotASpellDiskError(StateMachine executor) {
        super(executor, "error.not_a_spell_disk");
    }
}
