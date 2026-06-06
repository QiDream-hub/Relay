package qdream.relay.operations.world;

import qdream.relay.engine.OperationRegistry;

/**
 * 世界交互操作初始化器
 */
public class WorldOperationsInit {

    public static void register() {
        // 世界读取操作
        OperationRegistry.register("relay:get_block", new GetBlockOp())
                .requiresWorldInteractor(true)
                .register();

        // 世界写入操作
        OperationRegistry.register("relay:place_block", new PlaceBlockOp())
                .requiresWorldInteractor(true)
                .register();

        // TODO: 更多世界交互操作
        // - get_entity: 获取实体信息
        // - move_entity: 移动实体
        // - get_biome: 获取生物群系
        // - set_block_at: 在实体位置放置方块
    }
}
