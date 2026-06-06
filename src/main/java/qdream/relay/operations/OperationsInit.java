package qdream.relay.operations;

import qdream.relay.engine.OperationRegistry;
import qdream.relay.operations.base.*;
import qdream.relay.operations.arithmetic.*;
import qdream.relay.operations.logic.*;
import qdream.relay.operations.control.*;
import qdream.relay.operations.communication.*;
import qdream.relay.operations.list.*;
import qdream.relay.operations.world.*;

/**
 * 操作初始化器
 * 注册所有可用操作（使用完整命名空间 ID）
 */
public class OperationsInit {

    public static void register() {
        // 基础栈操作
        OperationRegistry.register("relay:pop", new PopOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:dup", new DupOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:swap", new SwapOp())
                .requiresWorldInteractor(false)
                .register();

        // 算术操作
        OperationRegistry.register("relay:add", new AddOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:sub", new SubOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:mul", new MulOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:div", new DivOp())
                .requiresWorldInteractor(false)
                .register();

        // 逻辑操作
        OperationRegistry.register("relay:and", new AndOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:or", new OrOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:not", new NotOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:eq", new EqOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:lt", new LtOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:gt", new GtOp())
                .requiresWorldInteractor(false)
                .register();

        // 控制流
        OperationRegistry.register("relay:eval", new EvalOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:if", new IfOp())
                .requiresWorldInteractor(false)
                .register();

        // 通信操作
        OperationRegistry.register("relay:send", new SendOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:recv", new RecvOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("relay:peek", new PeekOp())
                .requiresWorldInteractor(false)
                .register();

        // 控制流 - stop
        OperationRegistry.register("relay:stop", new StopOp())
                .requiresWorldInteractor(false)
                .register();

        // 列表操作
        ListOperationsInit.register();

        // 世界交互操作
        WorldOperationsInit.register();
    }
}
