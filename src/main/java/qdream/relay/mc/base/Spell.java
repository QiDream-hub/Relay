package qdream.relay.mc.base;

import qdream.relay.mc.OperationSignature;

/**
 * 指令类型基类（无状态单例操作）
 * 序列化/反序列化直接继承 Operation 的默认实现（仅处理 id）。
 */
public abstract class Spell extends Operation {
    // 操作消耗的能量
    protected final int energy;

    public Spell(String id, int cost,int energy, OperationSignature signature) {
        super(id, cost,signature);
        this.energy = energy;
    }

    public int getEnergy() {
        return energy;
    }
}
