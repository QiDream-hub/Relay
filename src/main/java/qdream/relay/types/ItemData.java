package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

/**
 * 物品类型
 * 封装 Minecraft 的 ItemStack，执行时自动压入数据栈
 */
public class ItemData extends Data {
    private final ItemStack itemStack;

    public ItemData(ItemStack itemStack) {
        super("relay:item", 0,
                DataSignature.builder()
                        .output("relay:item")
                        .field("item", "Item")
                        .build());
        this.itemStack = itemStack;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public boolean isEmpty() {
        return itemStack.isEmpty();
    }

    public int getCount() {
        return itemStack.getCount();
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag value = new CompoundTag();
        // 直接序列化 ItemStack 到 NBT
        if (!itemStack.isEmpty()) {
            var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            var key = registry.getKey(itemStack.getItem());
            if (key != null) {
                value.putString("id", key.toString());
            }
            value.putInt("count", itemStack.getCount());
        }
        tag.put("value", value);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag valueTag = tag.getCompound("value")
                .orElse(new CompoundTag());
        // 直接反序列化 ItemStack
        String id = valueTag.getString("id").orElse("minecraft:air");
        int count = valueTag.getInt("count").orElse(1);
        
        // 解析物品 ID（格式：namespace:path）
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        String[] parts = id.split(":", 2);
        String namespace = parts.length > 1 ? parts[0] : "minecraft";
        String path = parts.length > 1 ? parts[1] : parts[0];
        var identifier = Identifier.fromNamespaceAndPath(namespace, path);
        var item = registry.getOptional(identifier);
        ItemStack stack = item.map(i -> new ItemStack(i, count)).orElse(ItemStack.EMPTY);
        return new ItemData(stack);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject value = new JsonObject();
        JsonObject itemJson = new JsonObject();
        if (!itemStack.isEmpty()) {
            // 获取物品 ID（使用 BuiltInRegistries）
            var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            var key = registry.getKey(itemStack.getItem());
            itemJson.addProperty("id", key != null ? key.toString() : "minecraft:air");
            itemJson.addProperty("count", itemStack.getCount());
        } else {
            itemJson.addProperty("id", "minecraft:air");
            itemJson.addProperty("count", 0);
        }
        value.add("item", itemJson);
        json.add("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        JsonObject itemJson = json.get("value").getAsJsonObject()
                .get("item").getAsJsonObject();
        String id = itemJson.has("id") ? itemJson.get("id").getAsString() : "minecraft:air";
        int count = itemJson.has("count") ? itemJson.get("count").getAsInt() : 1;
        
        // 从注册表获取物品
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        String[] parts = id.split(":", 2);
        String namespace = parts.length > 1 ? parts[0] : "minecraft";
        String path = parts.length > 1 ? parts[1] : parts[0];
        var identifier = Identifier.fromNamespaceAndPath(namespace, path);
        ItemStack stack = ItemStack.EMPTY;
        var item = registry.getOptional(identifier);
        if (item.isPresent()) {
            stack = new ItemStack(item.get(), count);
        }
        return new ItemData(stack);
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof ItemData)) {
            return false;
        }
        ItemData that = (ItemData) other;
        return ItemStack.matches(this.itemStack, that.itemStack);
    }

    @Override
    public String asString() {
        if (itemStack.isEmpty()) {
            return "empty";
        }
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var key = registry.getKey(itemStack.getItem());
        String id = key != null ? key.toString() : "unknown";
        return id + " x" + itemStack.getCount();
    }
}
