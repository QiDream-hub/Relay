package qdream.relay.mc.base;

import qdream.relay.mc.OperationSignature;

/**
 * 指令类型基类（无状态单例操作）
 * 序列化/反序列化直接继承 Operation 的默认实现（仅处理 id）。
 */
public abstract class Spell extends Operation {
    protected final OperationSignature signature;

    public Spell(String id, int cost, OperationSignature signature) {
        super(id, cost);
        this.signature = signature;
    }

    public OperationSignature getSignature() {
        return signature;
    }
}
