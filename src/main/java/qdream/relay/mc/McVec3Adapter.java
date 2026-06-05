package qdream.relay.mc;

import qdream.relay.engine.Vector3;

/**
 * Minecraft Vec3 适配器
 * 实现 engine 的 Vector3 接口
 */
public class McVec3Adapter implements Vector3 {
    private final net.minecraft.world.phys.Vec3 vec3;

    public McVec3Adapter(net.minecraft.world.phys.Vec3 vec3) {
        this.vec3 = vec3;
    }

    @Override
    public double x() {
        return vec3.x;
    }

    @Override
    public double y() {
        return vec3.y;
    }

    @Override
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
