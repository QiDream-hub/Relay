package qdream.relay.mc.base;

import qdream.relay.mc.signature.OperationSignature;

/**
 * 指令类型基类（无状态单例操作）
 * 序列化/反序列化直接继承 Operation 的默认实现（仅处理 id）。
 */
public abstract class Spell extends Operation {
    // 操作消耗的能量
    protected final double energy;

    protected final OperationSignature signature;

    public Spell(String id, int cost, double energy, OperationSignature signature) {
        super(id, cost);
        this.energy = energy;
        this.signature = signature;
    }

    /**
     * 获取基础能量消耗
     * @return 基础能量值
     */
    public double getEnergy() {
        return energy;
    }

    @Override
    public OperationSignature getSignature() {
        return signature;
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof Spell that)) {
            return false;
        }

        return this.getId().equals(that.getId());

    }
}
