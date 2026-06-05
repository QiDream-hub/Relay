package qdream.relay.mc;

/**
 * Minecraft Vec3 适配器
 * 包装 Minecraft 的 Vec3 用于向量操作
 */
public class McVec3Adapter {
    private final net.minecraft.world.phys.Vec3 vec3;

    public McVec3Adapter(net.minecraft.world.phys.Vec3 vec3) {
        this.vec3 = vec3;
    }

    public double x() {
        return vec3.x;
    }

    public double y() {
        return vec3.y;
    }

    public double z() {
        return vec3.z;
    }

    @Override
    public String toString() {
        return vec3.toString();
    }

    public net.minecraft.world.phys.Vec3 getVec3() {
        return vec3;
    }

    public static McVec3Adapter fromVec3(net.minecraft.world.phys.Vec3 vec3) {
        return new McVec3Adapter(vec3);
    }
}
