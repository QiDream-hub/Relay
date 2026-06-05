package qdream.relay.operations.list;

import qdream.relay.engine.OperationRegistry;

/**
 * 列表操作初始化器
 */
public class ListOperationsInit {

    public static void register() {
        // 列表长度
        OperationRegistry.register("list_length", new ListLengthOp())
                .requiresWorldInteractor(false)
                .register();

        // 列表获取
        OperationRegistry.register("list_get", new ListGetOp())
                .requiresWorldInteractor(false)
                .register();

        // 列表设置
        OperationRegistry.register("list_set", new ListSetOp())
                .requiresWorldInteractor(false)
                .register();

        // 列表追加
        OperationRegistry.register("list_append", new ListAppendOp())
                .requiresWorldInteractor(false)
                .register();

        // TODO: 更多列表操作
        // - list_prepend: 在列表开头添加元素
        // - list_slice: 切片
        // - list_concat: 连接两个列表
        // - list_map: 映射
        // - list_fold: 折叠
        // - list_filter: 过滤
    }
}
