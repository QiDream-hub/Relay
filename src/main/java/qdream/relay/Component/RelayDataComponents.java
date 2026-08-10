package qdream.relay.Component;

import com.mojang.serialization.Codec;
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
     * 用于法术磁盘物品存储程序数据（向后兼容，不再使用）
     */
    public static final DataComponentType<CompoundTag> SPELL_PROGRAM = register(
            "spell_program",
            builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(createNbtStreamCodec())
    );

    /**
     * 法术程序组件 - 存储为 JSON 字符串
     * 用于法术磁盘物品存储程序数据（新格式）
     * 使用 32767 字符限制以支持较长的法术程序
     */
    public static final DataComponentType<String> SPELL_PROGRAM_JSON = register(
            "spell_program_json",
            builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(createStringStreamCodec(32767))
    );

    /**
     * 能量组件 - 存储为 Double
     * 用于能量模块物品存储能量值
     */
    public static final DataComponentType<Double> ENERGY = register(
            "energy",
            builder -> builder
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(createDoubleStreamCodec())
    );

    /**
     * 计算核心间隔组件 - 存储为 Integer
     * 用于 computing_core 物品存储执行间隔（tick 间隔）
     */
    public static final DataComponentType<Integer> INTERVAL = register(
            "interval",
            builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(createIntStreamCodec())
    );

    /**
     * 计算核心能量消耗组件 - 存储为 Double
     * 用于 computing_core 物品存储每次执行的能量消耗
     */
    public static final DataComponentType<Double> ENERGY_COST = register(
            "energy_cost",
            builder -> builder
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(createDoubleStreamCodec())
    );

    /**
     * 工具外壳物品栏组件 - 存储为 CompoundTag
     * 用于 tool_shell 物品存储 4 个插槽的物品和 StateMachine 状态
     * 结构：{ inventory: [...], stateMachine: {...} }
     */
    public static final DataComponentType<CompoundTag> TOOL_SHELL_DATA = register(
            "tool_shell_data",
            builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(createNbtStreamCodec())
    );

    /**
     * 工具外壳配置组件 - 存储为 CompoundTag
     * 用于 tool_shell 物品存储配置选项
     * 结构：{ useInventoryEnergy: boolean }
     */
    public static final DataComponentType<CompoundTag> TOOL_SHELL_CONFIG = register(
            "tool_shell_config",
            builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(createNbtStreamCodec())
    );

    /**
     * 工具外壳 Tick 状态组件 - 存储为 CompoundTag
     * 用于 tool_shell 物品存储 ShellTickHandler 的状态
     * 结构：{ tickCounter: int, initialized: boolean }
     */
    public static final DataComponentType<CompoundTag> TOOL_SHELL_TICK_STATE = register(
            "tool_shell_tick_state",
            builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(createNbtStreamCodec())
    );

    /**
     * 工具外壳会话 ID 组件 - 存储为 String (UUID)
     * 用于 tool_shell 物品存储当前会话的唯一标识符
     * 右键时生成，用于 PlayerShellData 的 Map key
     */
    public static final DataComponentType<String> TOOL_SHELL_SESSION_ID = register(
            "tool_shell_session_id",
            builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(createStringStreamCodec())
    );

    /**
     * 执行统计组件 - 存储为 CompoundTag
     * 用于 ShellContainer 存储程序执行统计信息
     * 结构：{ coreEnergy: double, operationEnergy: double, executedOperations: int, runCount: int }
     */
    public static final DataComponentType<CompoundTag> EXECUTION_STATS = register(
            "execution_stats",
            builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(createNbtStreamCodec())
    );

    /**
     * 世界交互器品阶组件 - 存储为 Integer
     * 用于 world_interactor 物品存储品阶（1-64）
     */
    public static final DataComponentType<Integer> WORLD_INTERACTOR_TIER = register(
            "world_interactor_tier",
            builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(createIntStreamCodec())
    );

    /**
     * 世界交互器交互距离组件 - 存储为 Double
     * 用于 world_interactor 物品存储交互距离（方块单位）
     */
    public static final DataComponentType<Double> WORLD_INTERACTION_RANGE = register(
            "world_interaction_range",
            builder -> builder
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(createDoubleStreamCodec())
    );

    /**
     * 创建 Double 的网络同步 StreamCodec
     */
    private static StreamCodec<FriendlyByteBuf, Double> createDoubleStreamCodec() {
        return new StreamCodec<FriendlyByteBuf, Double>() {
            @Override
            public Double decode(FriendlyByteBuf buf) {
                return buf.readDouble();
            }

            @Override
            public void encode(FriendlyByteBuf buf, Double value) {
                buf.writeDouble(value);
            }
        };
    }

    /**
     * 创建 Integer 的网络同步 StreamCodec
     */
    private static StreamCodec<FriendlyByteBuf, Integer> createIntStreamCodec() {
        return new StreamCodec<FriendlyByteBuf, Integer>() {
            @Override
            public Integer decode(FriendlyByteBuf buf) {
                return buf.readInt();
            }

            @Override
            public void encode(FriendlyByteBuf buf, Integer value) {
                buf.writeInt(value);
            }
        };
    }

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
     * 创建 String 的网络同步 StreamCodec
     * @param maxLength 最大字符串长度
     */
    private static StreamCodec<FriendlyByteBuf, String> createStringStreamCodec(int maxLength) {
        return new StreamCodec<FriendlyByteBuf, String>() {
            @Override
            public String decode(FriendlyByteBuf buf) {
                return buf.readUtf(maxLength);
            }

            @Override
            public void encode(FriendlyByteBuf buf, String value) {
                buf.writeUtf(value, maxLength);
            }
        };
    }

    /**
     * 创建 String 的网络同步 StreamCodec (默认用于 UUID，最大 36 字符)
     */
    private static StreamCodec<FriendlyByteBuf, String> createStringStreamCodec() {
        return createStringStreamCodec(36);
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
        var __ = ENERGY;
        var ___ = INTERVAL;
        var ____ = ENERGY_COST;
        var _____ = TOOL_SHELL_DATA;
        var ______ = TOOL_SHELL_CONFIG;
        var _______ = TOOL_SHELL_TICK_STATE;
        var ________ = TOOL_SHELL_SESSION_ID;
        var _________ = EXECUTION_STATS;
        var __________ = WORLD_INTERACTOR_TIER;
        var ___________ = WORLD_INTERACTION_RANGE;
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RelayDataComponents() {
    }
}
