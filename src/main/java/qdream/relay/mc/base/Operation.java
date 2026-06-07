package qdream.relay.mc.base;


import qdream.relay.engine.Executable;

public abstract class Operation implements Executable {
    protected final String id;
    protected final int cost;

    public Operation(String id, int cost) {
        this.id = id;
        this.cost = cost;
    }

    public String getId() {
        return id;
    }

    public int getCost() {
        return cost;
    }
}
