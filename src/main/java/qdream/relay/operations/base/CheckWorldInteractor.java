package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BooleanData;

/**
 * GetWorldInteractor 操作 - 获取世界交互器
 *
 * 演示如何使用 StateMachine 的上下文功能传递世界相关数据
 *
 * 弹出：无
 * 压入：boolean (是否有世界交互器)
 *
 * 示例用法：
 * 1. 检查是否有世界交互器：操作内部检查 hasContext("worldInteractor")
 * 2. 获取世界交互器：使用 getContext("worldInteractor", ItemStack.class)
 * 3. 处理结果：返回 boolean 表示是否有有效的世界交互器
 */
public class CheckWorldInteractor extends Instruction {

    public CheckWorldInteractor() {
        super("relay:check_world_interactor", 1, 0.25, OperationSignature.builder()
                .producesToData("hasInteractor", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        try {
            OperationHelpers.checkWorldInteractor(executor, id);
            executor.pushData(new BooleanData(true));
        } catch (Exception e) {
            executor.pushData(new BooleanData(false));
        }
    }

}
