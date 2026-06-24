package qdream.relay.operations.base;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.ContainerIota;

/**
 * GetSelf 操作 - 获取自身外壳容器
 *
 * 从上下文中获取 shellContainer 引用，并将其作为 ContainerIota 压入数据栈
 *
 * 弹出：无
 * 压入：container (自身外壳容器)
 *
 * 示例用法：
 * 1. 获取自身引用：getself
 * 2. 配合其他操作使用：getself some-container-op
 */
public class GetSelfOp extends Spell {

    public GetSelfOp() {
        super("relay:get_self", 1, 1, OperationSignature.builder()
                .output("relay:container")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 从上下文中获取 shellContainer
        ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);

        if (container == null) {
            executor.triggerMishap("无法获取自身容器：上下文缺失");
            return;
        }

        // 将容器作为 ContainerIota 压入数据栈
        executor.pushData(new ContainerIota(container));
    }
}
