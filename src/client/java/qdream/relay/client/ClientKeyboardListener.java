package qdream.relay.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import qdream.relay.tools.KeyboardListenerProvider;

/**
 * 客户端键盘监听器实现
 */
public class ClientKeyboardListener implements KeyboardListenerProvider {

    /**
     * 按键状态常量
     */
    private static final int KEY_UP = 0;
    private static final int KEY_PRESSED = 1;
    private static final int KEY_RELEASED = 2;

    /**
     * GLFW 键码范围常量
     */
    private static final int GLFW_KEY_FIRST = 32;  // GLFW_KEY_SPACE
    private static final int GLFW_KEY_LAST = 347;

    /**
     * 实体按键状态映射
     */
    private static final Map<Entity, int[]> ENTITY_KEY_STATES = new WeakHashMap<>();

    /**
     * 当前正在监听的实体集合
     */
    private static final Set<Entity> LISTENING_ENTITIES = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * 注册客户端监听器
     */
    public static void register() {
        ClientKeyboardListener listener = new ClientKeyboardListener();
        qdream.relay.tools.KeyboardListener.setProvider(listener);
    }

    @Override
    public void startListening(Entity entity) {
        if (entity == null) return;

        LISTENING_ENTITIES.add(entity);
        if (!ENTITY_KEY_STATES.containsKey(entity)) {
            // 扩展到 520 以包含鼠标按键 (512-519)
            // 键盘键码范围：32-347 (GLFW 定义的有效范围)
            // 鼠标键码范围：512-519 (自定义映射)
            ENTITY_KEY_STATES.put(entity, new int[520]);
        }
    }

    @Override
    public void stopListening(Entity entity) {
        LISTENING_ENTITIES.remove(entity);
        ENTITY_KEY_STATES.remove(entity);
    }

    @Override
    public int getKeyState(Entity entity, int keyCode) {
        int[] states = ENTITY_KEY_STATES.get(entity);
        if (states == null) {
            return KEY_UP;
        }
        if (keyCode < 0 || keyCode >= states.length) {
            return KEY_UP;
        }
        return states[keyCode];
    }

    @Override
    public List<Integer> getAllPressedKeys(Entity entity) {
        int[] states = ENTITY_KEY_STATES.get(entity);
        if (states == null) {
            return List.of();
        }

        List<Integer> pressedKeys = new ArrayList<>();

        // 检测键盘按键 (32-347，GLFW 定义的有效范围)
        for (int keyCode = GLFW_KEY_FIRST; keyCode <= GLFW_KEY_LAST; keyCode++) {
            if (states[keyCode] == KEY_PRESSED) {
                pressedKeys.add(keyCode);
            }
        }

        // 检测鼠标按键 (512-519)
        for (int button = 0; button < 8; button++) {
            int mouseKeyCode = 512 + button;
            if (mouseKeyCode < states.length && states[mouseKeyCode] == KEY_PRESSED) {
                pressedKeys.add(mouseKeyCode);
            }
        }

        return pressedKeys;
    }

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            updateKeyStates(client);
        });
    }

    private static void updateKeyStates(Minecraft client) {
        var player = client.player;
        if (player == null) return;

        long windowHandle = GLFW.glfwGetCurrentContext();

        LISTENING_ENTITIES.removeIf(entity -> entity.isRemoved() || !entity.isAlive());

        for (Entity entity : LISTENING_ENTITIES) {
            int[] states = ENTITY_KEY_STATES.get(entity);
            if (states == null) continue;

            // 更新键盘按键状态 (32-347，GLFW 定义的有效范围)
            // GLFW_KEY_SPACE = 32, GLFW_KEY_LAST = 347
            for (int keyCode = GLFW_KEY_FIRST; keyCode <= GLFW_KEY_LAST; keyCode++) {
                int currentState = states[keyCode];
                int glfwState = GLFW.glfwGetKey(windowHandle, keyCode);
                boolean isPressed = glfwState == GLFW.GLFW_PRESS;

                if (isPressed) {
                    if (currentState == KEY_UP || currentState == KEY_RELEASED) {
                        states[keyCode] = KEY_PRESSED;
                    }
                } else {
                    if (currentState == KEY_PRESSED) {
                        states[keyCode] = KEY_RELEASED;
                    } else if (currentState == KEY_RELEASED) {
                        states[keyCode] = KEY_UP;
                    }
                }
            }

            // 更新鼠标按键状态 (512-519)
            for (int button = 0; button < 8; button++) {
                int mouseKeyCode = 512 + button;
                int currentState = states[mouseKeyCode];
                int glfwState = GLFW.glfwGetMouseButton(windowHandle, button);
                boolean isPressed = glfwState == GLFW.GLFW_PRESS;

                if (isPressed) {
                    if (currentState == KEY_UP || currentState == KEY_RELEASED) {
                        states[mouseKeyCode] = KEY_PRESSED;
                    }
                } else {
                    if (currentState == KEY_PRESSED) {
                        states[mouseKeyCode] = KEY_RELEASED;
                    } else if (currentState == KEY_RELEASED) {
                        states[mouseKeyCode] = KEY_UP;
                    }
                }
            }
        }
    }
}
