package qdream.relay.items;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import qdream.relay.Relay;
import qdream.relay.engine.IExecutable;
import qdream.relay.mc.McIota;
import qdream.relay.mc.NbtSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 自定义 DataComponent 类型注册
 */
public class RelayDataComponents {

    /**
     * ListTag 的 Codec 实现 - 仅用于 NBT 持久化
     */
    private static final Codec<ListTag> NBT_CODEC = new Codec<ListTag>() {
        @Override
        public <T> DataResult<Pair<ListTag, T>> decode(DynamicOps<T> ops, T input) {
            if (ops instanceof NbtOps) {
                if (input instanceof ListTag listTag) {
                    return DataResult.success(Pair.of(listTag, ops.empty()));
                }
                return DataResult.error(() -> "Expected ListTag, got " + input);
            }
            // 非 NBT 环境尝试从其他格式转换
            return DataResult.error(() -> "Unsupported ops type: " + ops);
        }

        @Override
        public <T> DataResult<T> encode(ListTag input, DynamicOps<T> ops, T prefix) {
            if (ops instanceof NbtOps) {
                return DataResult.success((T) input);
            }
            return DataResult.error(() -> "Unsupported ops type: " + ops);
        }
    };

    /**
     * 法术程序组件的 StreamCodec - 用于网络同步
     */
    private static final StreamCodec<ByteBuf, ListTag> STREAM_CODEC = new StreamCodec<ByteBuf, ListTag>() {
        @Override
        public ListTag decode(ByteBuf buf) {
            try {
                Tag tag = FriendlyByteBuf.readNbt(buf, NbtAccounter.unlimitedHeap());
                if (tag instanceof ListTag listTag) {
                    return listTag;
                }
                if (tag == null) {
                    return new ListTag();
                }
                throw new RuntimeException("Expected ListTag, got " + tag.getClass());
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode ListTag", e);
            }
        }

        @Override
        public void encode(ByteBuf buf, ListTag tag) {
            try {
                FriendlyByteBuf.writeNbt(buf, tag);
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode ListTag", e);
            }
        }
    };

    /**
     * 法术程序组件 - 存储为 ListTag (NBT 列表)
     */
    public static final DataComponentType<ListTag> SPELL_PROGRAM = register(
            "spell_program",
            builder -> builder
                    .persistent(NBT_CODEC)
                    .networkSynchronized(STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Relay.MOD_ID, name)
        );
        DataComponentType<T> type = builderOperator.apply(DataComponentType.builder()).build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, type);
    }

    public static void register() {
        // 静态初始化时自动注册
    }

    /**
     * 获取物品的法术程序
     */
    public static List<IExecutable> getProgram(ItemStack stack) {
        ListTag listTag = stack.get(SPELL_PROGRAM);
        if (listTag == null) {
            return List.of();
        }
        List<IExecutable> result = new ArrayList<>();
        for (Tag element : listTag) {
            if (element instanceof CompoundTag compoundTag) {
                result.add(NbtSerializer.deserializeStatic(compoundTag));
            }
        }
        return result;
    }

    /**
     * 设置物品的法术程序
     */
    public static void setProgram(ItemStack stack, List<IExecutable> program) {
        ListTag listTag = new ListTag();
        for (IExecutable iota : program) {
            if (iota instanceof McIota mcIota) {
                listTag.add(NbtSerializer.serializeStatic(mcIota));
            } else {
                throw new RuntimeException("不支持的 IExecutable 类型：" + iota.getClass());
            }
        }
        stack.set(SPELL_PROGRAM, listTag);
    }

    /**
     * 检查物品是否有法术程序
     */
    public static boolean hasProgram(ItemStack stack) {
        return stack.has(SPELL_PROGRAM);
    }

    /**
     * 获取程序大小
     */
    public static int getProgramSize(ItemStack stack) {
        ListTag listTag = stack.get(SPELL_PROGRAM);
        return listTag == null ? 0 : listTag.size();
    }

    /**
     * 清空物品的法术程序
     */
    public static void clear(ItemStack stack) {
        stack.remove(SPELL_PROGRAM);
    }
}
