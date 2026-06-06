package qdream.relay.operations;

import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationSignature;

/**
 * 操作抽象基类
 * 提供 getId(), getCost(), getSignature(), toJson(), fromJson() 的默认实现
 * 子类只需实现 execute() 和定义常量
 */
public abstract class AbstractOperation implements Executable {
    protected final String id;
    protected final int cost;
    protected final OperationSignature signature;

    protected AbstractOperation(String id, int cost, OperationSignature signature) {
        this.id = id;
        this.cost = cost;
        this.signature = signature;
    }

    @Override
    public String getId() {
        return id;
    }

    public int getCost() {
        return cost;
    }

    public OperationSignature getSignature() {
        return signature;
    }

}
