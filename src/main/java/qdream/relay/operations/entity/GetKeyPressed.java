package qdream.relay.operations.entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.KeyboardListener;
import qdream.relay.types.EntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取按下按键操作
 * 检测实体（玩家）当前按下的所有键盘和鼠标按键，返回键码列表
 *
 * 弹出：
 * - entity (要检测的实体，通常是玩家)
 *
 * 压入：
 * - list (按下的键码列表，如果没有按键按下则返回空列表)
 *
 * 说明：
 * - 该操作会自动开始监听指定实体
 * - 返回所有当前按下的按键键码列表（GLFW 键码）
 * - 如果没有按键按下，返回空列表
 * - 列表中的键码按从小到大排序
 *
 * 键码范围：
 * - 键盘：0-511
 * - 鼠标：512-519 (512=左键，513=右键，514=中键)
 *
 * 常见键盘键码：
 * - A-Z: 65-90
 * - 0-9: 48-57
 * - 空格：32
 * - Enter: 257
 * - Escape: 256
 *
 * 示例用法：
 * 1. 获取所有按下的按键：get_owner get_key_pressed
 * 2. 检查是否有按键按下：get_owner get_key_pressed list_length 0 gt if { ... }
 * 3. 获取第一个按下的键：get_owner get_key_pressed 0 list_get
 * 4. 检查是否按下 E 键：get_owner get_key_pressed 69 list_contains if { ... }
 */
public class GetKeyPressed extends Instruction {

    public GetKeyPressed() {
        super("relay:get_key_pressed", 1, 0.1, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("keys", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出实体
        EntityData entityData = StackHelpers.popEntity(executor, id);

        // 获取实体
        var entity = entityData.getEntity();

        // 自动开始监听该实体
        if (entity != null) {
            KeyboardListener.startListening(entity);
        }

        // 获取所有按下的按键
        List<Integer> pressedKeys = KeyboardListener.getAllPressedKeys(entity);

        // 创建列表并压入
        List<qdream.relay.engine.Executable> items = new ArrayList<>();
        for (int keyCode : pressedKeys) {
            items.add(new NumberData(keyCode));
        }
        ListData listData = new ListData(items);

        executor.pushData(listData);
    }
}
