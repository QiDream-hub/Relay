package qdream.relay.entities;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import qdream.relay.Relay;

/**
 * Relay 模组实体类型注册表
 * 
 * <h3>设计模式</h3>
 * <ul>
 * <li>使用静态 final 字段存储注册后的 EntityType</li>
 * <li>register() 方法触发静态块初始化（避免提前注册问题）</li>
 * <li>EntityType.Builder 链式构建，明确指定分类和属性</li>
 * </ul>
 */
public class RelayEntities {

    /**
     * Shell 实体类型 - 可编程的悬浮实体
     * 
     * <h3>属性</h3>
     * <ul>
     * <li>分类：MISC（杂项实体）</li>
     * <li>尺寸：0.6f x 0.6f</li>
     * <li>追踪范围：10 区块</li>
     * <li>同步间隔：20 tick</li>
     * <li>不可燃烧、不生成战利品</li>
     * </ul>
     */
    public static final EntityType<EntityShell> ENTITY_SHELL = register(
        "entity_shell",
        EntityType.Builder.<EntityShell>of(EntityShell::new, MobCategory.MISC)
            .sized(0.6f, 0.6f)
            .eyeHeight(0.3f)
            .clientTrackingRange(10)
            .updateInterval(20)
            .noLootTable()
    );

    /**
     * 注册实体类型到注册表
     * 
     * @param name 实体名称（命名空间自动使用 MOD_ID）
     * @param builder EntityType.Builder 实例
     * @return 注册后的 EntityType
     */
    private static <T extends Entity> EntityType<T> register(
        String name,
        EntityType.Builder<T> builder
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    /**
     * 初始化注册表
     * 
     * <p>此方法必须被主类调用以触发静态块初始化。</p>
     * <p>调用顺序：在 FabricLoader 初始化阶段调用，早于任何实体生成逻辑。</p>
     */
    public static void register() {
        // 静态块已执行注册
    }
}
