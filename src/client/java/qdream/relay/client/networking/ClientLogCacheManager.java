package qdream.relay.client.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 客户端日志缓存管理器
 * 
 * <p>
 * 负责管理客户端侧的 Shell 日志缓存，实现以下功能：
 * </p>
 * 
 * <ul>
 * <li><strong>零持久化</strong>：日志仅存储在内存中，客户端重启后自动清空</li>
 * <li><strong>维度隔离</strong>：使用 {@code Map<String, Queue<Component>>} 存储，Key 为 "维度 ID:坐标" 字符串</li>
 * <li><strong>单条推送</strong>：服务端每次推送单条日志，减少网络数据包大小</li>
 * <li><strong>自动清理</strong>：方块被破坏时自动清除对应缓存</li>
 * <li><strong>线程安全</strong>：使用并发集合支持多线程访问</li>
 * </ul>
 * 
 * <h3>使用场景</h3>
 * <ol>
 * <li>客户端启动时自动创建空缓存</li>
 * <li>接收服务端 S2C_ShellLogPushPayload 推送单条日志</li>
 * <li>GUI 展示时从缓存读取日志</li>
 * <li>方块破坏时接收 S2C_ClearLogsPayload 清理缓存</li>
 * </ol>
 * 
 * <h3>内存管理</h3>
 * <p>
 * 每个方块最多缓存 200 条日志，超出时自动移除最旧的日志。
 * 缓存仅在客户端运行期间存在，退出游戏后自动释放。
 * </p>
 * 
 * <h3>Key 格式</h3>
 * <p>
 * {@code "minecraft:overworld/100,64,200"} - 维度命名空间：维度路径/X,Y,Z
 * </p>
 */
public class ClientLogCacheManager {
    
    private static final int MAX_LOGS_PER_BLOCK = 200;
    
    /**
     * 日志缓存映射
     * Key: String - "维度 ID/坐标 X,Y,Z" 格式，例如 "minecraft:overworld/100,64,200"
     * Value: ConcurrentLinkedQueue<Component> - 日志队列（按时间顺序）
     * 
     * 使用字符串 Key 而不是 BlockPos 的原因：
     * 1. 支持不同维度的相同坐标隔离（例如主世界和下界的 100,64,200）
     * 2. 简化网络包传输（直接传字符串而不是 BlockPos + 维度）
     */
    private static final Map<String, ConcurrentLinkedQueue<Component>> LOG_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 私有构造函数，防止实例化
     */
    private ClientLogCacheManager() {
    }
    
    /**
     * 清空所有缓存（客户端启动时调用）
     */
    public static void clearAll() {
        LOG_CACHE.clear();
    }
    
    /**
     * 将 BlockPos 转换为带维度的字符串 Key
     * 
     * @param level 世界（用于获取维度）
     * @param pos   坐标
     * @return Key 字符串，格式："minecraft:overworld/100,64,200"
     */
    private static String toKey(Level level, BlockPos pos) {
        if (level == null) {
            return "unknown/" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        }
        ResourceKey<Level> dimensionKey = level.dimension();
        Identifier dimensionId = dimensionKey.identifier();
        return dimensionId.getNamespace() + ":" + dimensionId.getPath() + "/" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
    
    /**
     * 从字符串 Key 解析 BlockPos
     * 
     * @param key Key 字符串
     * @return BlockPos 坐标（不包含维度信息）
     */
    private static BlockPos fromKey(String key) {
        String[] parts = key.split("/");
        if (parts.length != 2) {
            return BlockPos.ZERO;
        }
        String[] coords = parts[1].split(",");
        if (coords.length != 3) {
            return BlockPos.ZERO;
        }
        return new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
    }
    
    /**
     * 添加单条日志到指定方块的缓存
     * 
     * @param level 世界（用于获取维度）
     * @param pos   方块坐标
     * @param log   日志内容
     */
    public static void addLog(Level level, BlockPos pos, Component log) {
        String key = toKey(level, pos);
        LOG_CACHE.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<Component> queue = LOG_CACHE.get(key);
        
        // 移除最旧的日志以维持容量限制
        while (queue.size() >= MAX_LOGS_PER_BLOCK) {
            queue.poll();
        }
        queue.offer(log);
    }
    
    /**
     * 获取指定方块的日志列表
     * 
     * @param level 世界（用于获取维度）
     * @param pos   方块坐标
     * @return 日志列表（按时间顺序）
     */
    public static List<Component> getLogs(Level level, BlockPos pos) {
        String key = toKey(level, pos);
        ConcurrentLinkedQueue<Component> queue = LOG_CACHE.get(key);
        if (queue == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(queue);
    }
    
    /**
     * 清空指定方块的日志缓存（方块被破坏时调用）
     * 
     * @param level 世界（用于获取维度）
     * @param pos   方块坐标
     */
    public static void clearLogs(Level level, BlockPos pos) {
        String key = toKey(level, pos);
        LOG_CACHE.remove(key);
    }
    
    /**
     * 检查指定方块是否有日志缓存
     * 
     * @param level 世界（用于获取维度）
     * @param pos   方块坐标
     * @return 如果有缓存返回 true
     */
    public static boolean hasLogs(Level level, BlockPos pos) {
        String key = toKey(level, pos);
        return LOG_CACHE.containsKey(key);
    }
    
    /**
     * 获取缓存的方块数量（调试用）
     * 
     * @return 缓存的方块数量
     */
    public static int getCachedBlockCount() {
        return LOG_CACHE.size();
    }
}
