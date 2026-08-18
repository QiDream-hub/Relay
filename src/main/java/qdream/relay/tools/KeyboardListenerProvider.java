package qdream.relay.tools;

import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 键盘监听器接口
 * 用于解耦主代码和客户端实现
 */
public interface KeyboardListenerProvider {
    /**
     * 开始监听实体的按键
     */
    void startListening(Entity entity);
    
    /**
     * 停止监听实体的按键
     */
    void stopListening(Entity entity);
    
    /**
     * 获取按键状态
     * @return 0=未按下，1=刚按下，2=刚释放
     */
    int getKeyState(Entity entity, int keyCode);
    
    /**
     * 获取所有按下的按键列表（包括键盘和鼠标）
     * @return 按下的键码列表
     */
    List<Integer> getAllPressedKeys(Entity entity);
    
    /**
     * 默认提供者（服务端，返回空列表）
     */
    KeyboardListenerProvider DEFAULT = new KeyboardListenerProvider() {
        @Override
        public void startListening(Entity entity) {}
        
        @Override
        public void stopListening(Entity entity) {}
        
        @Override
        public int getKeyState(Entity entity, int keyCode) {
            return 0; // 服务端返回未按下
        }
        
        @Override
        public List<Integer> getAllPressedKeys(Entity entity) {
            return List.of(); // 服务端返回空列表
        }
    };
}
