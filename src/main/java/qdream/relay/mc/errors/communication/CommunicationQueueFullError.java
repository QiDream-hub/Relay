package qdream.relay.mc.errors.communication;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 通信频道队列已满
 * <p>翻译键：{@code error.communication_queue_full}</p>
 */
public class CommunicationQueueFullError extends ExecutionException {

    public CommunicationQueueFullError(StateMachine executor) {
        super(executor, "error.communication_queue_full");
    }
}
