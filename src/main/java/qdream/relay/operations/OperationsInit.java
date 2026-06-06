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
        OperationRegistry.register("relay:pop", new PopOp());
        OperationRegistry.register("relay:dup", new DupOp());
        OperationRegistry.register("relay:swap", new SwapOp());

        // 算术操作
        OperationRegistry.register("relay:add", new AddOp());
        OperationRegistry.register("relay:sub", new SubOp());
        OperationRegistry.register("relay:mul", new MulOp());
        OperationRegistry.register("relay:div", new DivOp());

        // 逻辑操作
        OperationRegistry.register("relay:and", new AndOp());
        OperationRegistry.register("relay:or", new OrOp());
        OperationRegistry.register("relay:not", new NotOp());
        OperationRegistry.register("relay:eq", new EqOp());
        OperationRegistry.register("relay:lt", new LtOp());
        OperationRegistry.register("relay:gt", new GtOp());

        // 控制流
        OperationRegistry.register("relay:eval", new EvalOp());
        OperationRegistry.register("relay:if", new IfOp());
        OperationRegistry.register("relay:stop", new StopOp());

        // 通信操作
        OperationRegistry.register("relay:send", new SendOp());
        OperationRegistry.register("relay:recv", new RecvOp());
        OperationRegistry.register("relay:peek", new PeekOp());

        // 列表操作
        ListOperationsInit.register();

        // 世界交互操作
        WorldOperationsInit.register();
    }
}
