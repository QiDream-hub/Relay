# 基于对象自身的 NBT 序列化系统

## 概述

Relay 模组的 NBT 序列化系统已经集成到 `Data` 和 `Spell` 基类中。每个类型负责自己的序列化和反序列化逻辑，实现了面向对象的设计。

## 架构设计

### 核心类

**`Data` 抽象类** - 所有数据类型的基类
```java
public abstract class Data extends Operation {
    // 序列化为 NBT
    public abstract void toNbt(CompoundTag tag);
    
    // 从 NBT 反序列化
    public abstract Data fromNbt(CompoundTag tag);
}
```

**`Spell` 抽象类** - 所有操作的基类
```java
public abstract class Spell extends Operation {
    // 序列化（默认实现，子类可扩展）
    public void toNbt(CompoundTag tag) { }
    
    // 反序列化（操作是单例，返回自身）
    public Spell fromNbt(CompoundTag tag) { return this; }
}
```

**`OperationRegistry`** - 中央注册表
```java
// 序列化
Optional<CompoundTag> serializeToNbt(Executable exec)

// 反序列化
Optional<Executable> deserializeFromNbt(CompoundTag tag)
```

## 使用方式

### 序列化

```java
import qdream.relay.mc.OperationRegistry;
import qdream.relay.types.NumberIota;
import net.minecraft.nbt.CompoundTag;

// 创建数据类型实例
NumberIota number = new NumberIota(42);

// 通过注册表序列化（调用对象自身的 toNbt 方法）
Optional<CompoundTag> tagOpt = OperationRegistry.serializeToNbt(number);
tagOpt.ifPresent(tag -> {
    // tag 包含：{id: "relay:number", value: 42}
    compoundTag.put("myData", tag);
});
```

### 反序列化

```java
import qdream.relay.mc.OperationRegistry;
import qdream.relay.engine.Executable;

// 从 NBT 读取
CompoundTag tag = compoundTag.getCompound("myData");
Optional<Executable> execOpt = OperationRegistry.deserializeFromNbt(tag);
execOpt.ifPresent(exec -> {
    // exec 是反序列化后的 Executable 实例
    stateMachine.pushData(exec);
});
```

### 序列化列表

```java
import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import net.minecraft.nbt.ListTag;
import java.util.List;

List<Executable> program = getProgram();
ListTag listTag = new ListTag();

// 递归序列化每个元素（调用各自的 toNbt 方法）
for (Executable exec : program) {
    OperationRegistry.serializeToNbt(exec).ifPresent(listTag::add);
}
compoundTag.put("program", listTag);
```

### 反序列化列表

```java
import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import net.minecraft.nbt.ListTag;
import java.util.ArrayList;
import java.util.List;

ListTag listTag = compoundTag.getList("program").orElse(new ListTag());
List<Executable> program = new ArrayList<>();

for (Tag element : listTag) {
    if (element instanceof CompoundTag tag) {
        OperationRegistry.deserializeFromNbt(tag).ifPresent(program::add);
    }
}
```

## 已实现的数据类型序列化

| 类型 ID | 类名 | NBT 字段 |
|---------|------|----------|
| `relay:number` | `NumberIota` | `value` (int 或 double) |
| `relay:boolean` | `BooleanIota` | `value` (boolean) |
| `relay:string` | `StringIota` | `value` (string) |
| `relay:vector` | `VectorIota` | `x`, `y`, `z` (double) |
| `relay:entity` | `EntityIota` | `value` (UUID string) |
| `relay:null` | `NullIota` | 无字段 |
| `relay:list` | `ListIota` | `value` (ListTag，递归序列化) |

## 操作的序列化

操作（`Spell` 子类）是单例，序列化时只保存 `id` 字段：

```java
// 序列化
CompoundTag tag = new CompoundTag();
tag.putString("id", "relay:add");
// 操作的 toNbt() 默认实现为空，因为操作无状态

// 反序列化
Optional<Executable> opOpt = OperationRegistry.deserializeFromNbt(tag);
// 返回注册表中的单例实例
```

## 扩展自定义数据类型

其他模组可以创建自己的数据类型，继承 `Data` 类并实现序列化方法：

```java
import qdream.relay.mc.base.Data;
import qdream.relay.engine.StateMachine;
import net.minecraft.nbt.CompoundTag;

public class CustomData extends Data {
    private final String customValue;

    public CustomData(String value) {
        super("mymod:custom", 0);
        this.customValue = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public void toNbt(CompoundTag tag) {
        tag.putString("customValue", customValue);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        String value = tag.getString("customValue").orElse("");
        return new CustomData(value);
    }
}

// 注册
OperationRegistry.registerData("mymod:custom", () -> new CustomData("default"));
```

## 优势

1. **面向对象** - 每个类型负责自己的序列化逻辑
2. **类型安全** - 编译时检查序列化方法
3. **可扩展** - 其他模组可以轻松添加自定义类型
4. **简洁** - 不需要额外的序列化器注册
5. **统一** - 所有类型使用相同的接口
