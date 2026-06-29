package qdream.relay.core;

import qdream.relay.core.PlayerShellData;

/**
 * PlayerShellData 访问接口
 * 
 * <p>通过此接口访问玩家的 PlayerShellData，避免 Mixin 类型转换问题</p>
 * 
 * <p>使用方式：</p>
 * <pre>{@code
 * if (player instanceof PlayerShellDataAccessor accessor) {
 *     PlayerShellData data = accessor.relay$getShellData();
 *     data.tickAll();
 * }
 * }</pre>
 */
public interface PlayerShellDataAccessor {
    
    /**
     * 获取玩家的 PlayerShellData
     */
    PlayerShellData relay$getShellData();
}
