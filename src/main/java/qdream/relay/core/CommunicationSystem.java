package qdream.relay.core;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import qdream.relay.engine.Executable;
import qdream.relay.types.NullData;

/**
 * 通信系统
 * 管理跨外壳、跨维度的消息传递
 */
public class CommunicationSystem {
    /**
     * 全局频道 Map - 所有维度共享
     * 频道号 -> 消息队列
     */
    private static final Map<Integer, Queue<Executable>> CHANNELS = new ConcurrentHashMap<>();

    /**
     * 频道队列最大容量
     */
    public static final int MAX_QUEUE_SIZE = 1000;

    private CommunicationSystem() {}

    /**
     * 发送数据到频道
     * @param channel 频道号
     * @param data 数据
     * @return 成功返回 true，队列已满返回 false
     */
    public static boolean send(int channel, Executable data) {
        Queue<Executable> queue = CHANNELS.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>());

        if (queue.size() >= MAX_QUEUE_SIZE) {
            return false;
        }

        return queue.offer(data);
    }

    /**
     * 接收数据（出队）
     * @param channel 频道号
     * @return 数据或 null
     */
    public static Executable recv(int channel) {
        Queue<Executable> queue = CHANNELS.get(channel);
        if (queue == null || queue.isEmpty()) {
            return NullData.INSTANCE;
        }
        return queue.poll();
    }

    /**
     * 窥探数据（不出队）
     * @param channel 频道号
     * @return 数据或 null
     */
    public static Executable peek(int channel) {
        Queue<Executable> queue = CHANNELS.get(channel);
        if (queue == null || queue.isEmpty()) {
            return NullData.INSTANCE;
        }
        return queue.peek();
    }

    /**
     * 获取频道队列长度
     */
    public static int getQueueSize(int channel) {
        Queue<Executable> queue = CHANNELS.get(channel);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 检查频道是否存在
     */
    public static boolean hasChannel(int channel) {
        return CHANNELS.containsKey(channel);
    }

    /**
     * 清除频道（用于测试）
     */
    public static void clearChannel(int channel) {
        CHANNELS.remove(channel);
    }

    /**
     * 清除所有频道（用于测试）
     */
    public static void clearAll() {
        CHANNELS.clear();
    }

    /**
     * 获取所有频道号
     */
    public static java.util.Set<Integer> getAllChannels() {
        return CHANNELS.keySet();
    }
}
