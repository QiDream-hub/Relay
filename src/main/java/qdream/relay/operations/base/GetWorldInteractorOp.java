package qdream.relay.operations.base;

import net.minecraft.world.item.ItemStack;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanIota;

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
public class GetWorldInteractorOp extends Spell {

    public GetWorldInteractorOp() {
        super("relay:get_world_interactor", 1, 1, OperationSignature.builder()
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 从上下文中检查是否有世界交互器
        if (!executor.hasContext("worldInteractor")) {
            // 没有上下文，返回 false
            executor.pushData(new BooleanIota(false));
            return;
        }

        // 获取世界交互器（类型安全获取）
        ItemStack interactor = executor.getContext("worldInteractor", ItemStack.class);
        
        // 检查是否有效
        boolean hasInteractor = (interactor != null && !interactor.isEmpty());
        executor.pushData(new BooleanIota(hasInteractor));
    }

}
