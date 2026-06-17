package qdream.relay.Component;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import qdream.relay.Relay;

/**
 * Relay Mod 自定义 DataComponent 注册
 * 使用 26.1.2 标准注册方式
 */
public class RelayDataComponents {

    /**
     * 法术程序组件 - 存储为 CompoundTag (内部包含 ListTag)
     * 用于法术磁盘物品存储程序数据
     */
    public static final DataComponentType<CompoundTag> SPELL_PROGRAM = register(
            "spell_program",
            builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(createNbtStreamCodec())
    );

    /**
     * 创建 NBT CompoundTag 的网络同步 StreamCodec
     */
    private static StreamCodec<FriendlyByteBuf, CompoundTag> createNbtStreamCodec() {
        return new StreamCodec<FriendlyByteBuf, CompoundTag>() {
            @Override
            public CompoundTag decode(FriendlyByteBuf buf) {
                try {
                    return buf.readNbt();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode CompoundTag", e);
                }
            }

            @Override
            public void encode(FriendlyByteBuf buf, CompoundTag tag) {
                try {
                    buf.writeNbt(tag);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encode CompoundTag", e);
                }
            }
        };
    }

    /**
     * 注册 DataComponentType
     * @param name 组件名称
     * @param builderOperator 构建器操作符，用于配置持久化和网络同步
     */
    private static <T> DataComponentType<T> register(
            String name,
            java.util.function.UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Relay.MOD_ID, name)
        );
        DataComponentType<T> type = builderOperator
                .apply(DataComponentType.builder())
                .build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, type);
    }

    /**
     * 注册所有自定义 DataComponent
     * 应在模组初始化时调用
     */
    public static void register() {
        // 静态初始化时自动注册所有组件
        // 访问任意组件即可触发类加载
        var _ = SPELL_PROGRAM;
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RelayDataComponents() {
    }
}
