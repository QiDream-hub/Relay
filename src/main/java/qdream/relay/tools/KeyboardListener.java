package qdream.relay.tools;

import net.minecraft.world.entity.Entity;

/**
 * 键盘监听器代理
 * 主代码包通过此类访问键盘监听功能，实际实现由客户端提供
 */
public class KeyboardListener {
    
    /**
     * 实际提供者（客户端注册）
     */
    private static KeyboardListenerProvider provider = KeyboardListenerProvider.DEFAULT;
    
    /**
     * 设置提供者（客户端调用）
     */
    public static void setProvider(KeyboardListenerProvider provider) {
        KeyboardListener.provider = provider != null ? provider : KeyboardListenerProvider.DEFAULT;
    }
    
    /**
     * 开始监听实体的按键
     */
    public static void startListening(Entity entity) {
        provider.startListening(entity);
    }
    
    /**
     * 停止监听实体的按键
     */
    public static void stopListening(Entity entity) {
        provider.stopListening(entity);
    }
    
    /**
     * 获取按键状态
     * @return 0=未按下 (KEY_UP), 1=刚按下 (KEY_PRESSED), 2=刚释放 (KEY_RELEASED)
     */
    public static int getKeyState(Entity entity, int keyCode) {
        return provider.getKeyState(entity, keyCode);
    }
    
    /**
     * 获取所有按下的按键列表（包括键盘和鼠标）
     * @return 按下的键码列表
     */
    public static java.util.List<Integer> getAllPressedKeys(Entity entity) {
        return provider.getAllPressedKeys(entity);
    }
    
    /**
     * 按键状态常量
     */
    public static final int KEY_UP = 0;
    public static final int KEY_PRESSED = 1;
    public static final int KEY_RELEASED = 2;
}
