package qdream.relay.engine;

/**
 * 三维向量接口
 * 抽象向量表示，使引擎不依赖 Minecraft 的 Vec3
 */
public interface Vector3 {
    double x();
    double y();
    double z();
}
