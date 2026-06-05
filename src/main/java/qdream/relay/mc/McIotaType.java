package qdream.relay.mc;

/**
 * Minecraft Iota 类型枚举
 */
public enum McIotaType {
    NUMBER,
    BOOLEAN,
    VECTOR,
    STRING,
    ENTITY,
    LIST,
    NULL,
    ANY;

    /**
     * 获取小写类型名（与 IData.getType() 兼容）
     */
    public String toLowerCase() {
        return name().toLowerCase();
    }
}
