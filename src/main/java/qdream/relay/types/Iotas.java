package qdream.relay.types;

import qdream.relay.engine.Executable;
import qdream.relay.mc.McVec3Adapter;

import java.util.List;
import java.util.UUID;

/**
 * Iota 工厂类
 * 提供便捷的静态方法创建各种类型的 Iota
 */
public final class Iotas {

    private Iotas() {}

    public static Executable number(double value) {
        return new NumberIota(value);
    }

    public static Executable number(int value) {
        return new NumberIota(value);
    }

    public static Executable booleanIota(boolean value) {
        return new BooleanIota(value);
    }

    public static Executable string(String value) {
        return new StringIota(value);
    }

    public static Executable vector(McVec3Adapter value) {
        return new VectorIota(value);
    }

    public static Executable vector(net.minecraft.world.phys.Vec3 value) {
        return new VectorIota(value);
    }

    public static Executable entity(UUID value) {
        return new EntityIota(value);
    }

    public static Executable nullIota() {
        return NullIota.INSTANCE;
    }

    public static Executable list(List<Executable> items) {
        return new ProgramBlock(items);
    }

    public static Executable operation(String opId) {
        return new Operation(opId);
    }

    /**
     * 从 Executable 转换为具体的类型
     */
    public static NumberIota asNumber(Executable exec) {
        if (exec instanceof NumberIota n) {
            return n;
        }
        throw new IllegalArgumentException("期望 number 类型，实际为：" + exec.getId());
    }

    public static BooleanIota asBoolean(Executable exec) {
        if (exec instanceof BooleanIota b) {
            return b;
        }
        throw new IllegalArgumentException("期望 boolean 类型，实际为：" + exec.getId());
    }

    public static StringIota asString(Executable exec) {
        if (exec instanceof StringIota s) {
            return s;
        }
        throw new IllegalArgumentException("期望 string 类型，实际为：" + exec.getId());
    }

    public static VectorIota asVector(Executable exec) {
        if (exec instanceof VectorIota v) {
            return v;
        }
        throw new IllegalArgumentException("期望 vector 类型，实际为：" + exec.getId());
    }

    public static EntityIota asEntity(Executable exec) {
        if (exec instanceof EntityIota e) {
            return e;
        }
        throw new IllegalArgumentException("期望 entity 类型，实际为：" + exec.getId());
    }

    public static ProgramBlock asList(Executable exec) {
        if (exec instanceof ProgramBlock l) {
            return l;
        }
        throw new IllegalArgumentException("期望 list 类型，实际为：" + exec.getId());
    }
}
