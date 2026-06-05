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
 * 注册所有可用操作
 */
public class OperationsInit {

    public static void register() {
        // 基础栈操作
        OperationRegistry.register("pop", new PopOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("dup", new DupOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("swap", new SwapOp())
                .requiresWorldInteractor(false)
                .register();

        // 算术操作
        OperationRegistry.register("add", new AddOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("sub", new SubOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("mul", new MulOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("div", new DivOp())
                .requiresWorldInteractor(false)
                .register();

        // 逻辑操作
        OperationRegistry.register("and", new AndOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("or", new OrOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("not", new NotOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("eq", new EqOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("lt", new LtOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("gt", new GtOp())
                .requiresWorldInteractor(false)
                .register();

        // 控制流
        OperationRegistry.register("eval", new EvalOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("if", new IfOp())
                .requiresWorldInteractor(false)
                .register();

        // 通信操作
        OperationRegistry.register("send", new SendOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("recv", new RecvOp())
                .requiresWorldInteractor(false)
                .register();

        OperationRegistry.register("peek", new PeekOp())
                .requiresWorldInteractor(false)
                .register();

        // 控制流 - stop
        OperationRegistry.register("stop", new StopOp())
                .requiresWorldInteractor(false)
                .register();

        // 列表操作
        ListOperationsInit.register();

        // 世界交互操作
        WorldOperationsInit.register();
    }
}
