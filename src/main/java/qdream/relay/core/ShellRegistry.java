package qdream.relay.core;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import qdream.relay.blocks.entity.custom.ShellBlockEntity;

/**
 * 外壳注册表
 * 管理所有外壳的注册、注销和访问
 */
public class ShellRegistry {
    
    /**
     * 弱引用 Map，避免阻止 GC
     * 只在服务端维护
     */
    private static final Map<ShellBlockEntity, BlockPos> SHELLS = new WeakHashMap<>();

    private ShellRegistry() {}

    /**
     * 注册外壳
     */
    public static void register(ShellBlockEntity shell, BlockPos pos) {
        SHELLS.put(shell, pos.immutable());
    }

    /**
     * 注销外壳
     */
    public static void unregister(ShellBlockEntity shell) {
        SHELLS.remove(shell);
    }

    /**
     * 获取所有注册的外壳
     */
    public static Map<ShellBlockEntity, BlockPos> getAllShells() {
        return Map.copyOf(SHELLS);
    }

    /**
     * 获取外壳数量
     */
    public static int size() {
        return SHELLS.size();
    }

    /**
     * 清除所有注册（用于测试）
     */
    public static void clear() {
        SHELLS.clear();
    }
}
