package qdream.relay.core;

/**
 * 程序执行统计信息
 * <p>
 * 记录单次程序运行的能量消耗和操作执行情况
 * </p>
 */
public class ExecutionStats {

    /** 运算核心消耗的能量 */
    private double coreEnergyCost;

    /** 基础操作消耗的能量（操作本身的 cost） */
    private double operationEnergyCost;

    /** 世界交互器额外消耗的能量 */
    private double worldInteractorEnergyCost;

    /** 实际执行的操作数量 */
    private int executedOperationCount;

    /** 经历的 tick 次数 */
    private int runCount;

    public ExecutionStats() {
        this.coreEnergyCost = 0.0;
        this.operationEnergyCost = 0.0;
        this.worldInteractorEnergyCost = 0.0;
        this.executedOperationCount = 0;
        this.runCount = 0;
    }

    /**
     * 添加核心基础能量消耗
     *
     * @param cost 核心消耗的能量
     */
    public void addCoreEnergy(double cost) {
        this.coreEnergyCost += cost;
    }

    /**
     * 添加操作基础能量消耗
     *
     * @param cost 操作本身的基础消耗
     */
    public void addOperationEnergy(double cost) {
        this.operationEnergyCost += cost;
    }

    /**
     * 添加世界交互器额外能量消耗
     *
     * @param cost 世界交互器的额外消耗
     */
    public void addWorldInteractorEnergy(double cost) {
        this.worldInteractorEnergyCost += cost;
    }

    /**
     * 添加能量消耗（旧方法，保留兼容性）
     *
     * @param coreCost      核心基础消耗
     * @param operationCost 操作额外消耗（世界交互器等）
     * @deprecated 使用 {@link #addEnergyCost(double, double, double)} 或分别调用各方法
     */
    @Deprecated
    public void addEnergyCost(double coreCost, double operationCost) {
        this.coreEnergyCost += coreCost;
        this.worldInteractorEnergyCost += operationCost;
    }

    /**
     * 添加能量消耗（新版本，三分离）
     *
     * @param coreCost           核心基础消耗
     * @param baseOperationCost  基础操作消耗
     * @param worldInteractorCost 世界交互器额外消耗
     */
    public void addEnergyCost(double coreCost, double baseOperationCost, double worldInteractorCost) {
        this.coreEnergyCost += coreCost;
        this.operationEnergyCost += baseOperationCost;
        this.worldInteractorEnergyCost += worldInteractorCost;
    }

    /**
     * 递增 tick 计数
     */
    public void incrementRunCount() {
        this.runCount++;
    }

    /**
     * 记录执行的操作
     *
     * @param count 操作数量
     */
    public void addExecutedOperations(int count) {
        this.executedOperationCount += count;
    }

    /**
     * 递增执行的操作数量
     *
     * @return 新的操作计数
     */
    public int incrementOperations() {
        this.executedOperationCount++;
        return this.executedOperationCount;
    }

    /**
     * 递增执行的操作数量（批量）
     *
     * @param count 操作数量
     * @return 新的操作计数
     */
    public int incrementOperations(int count) {
        this.executedOperationCount += count;
        return this.executedOperationCount;
    }

    /**
     * 重置统计信息（程序停止时调用）
     */
    public void reset() {
        this.coreEnergyCost = 0.0;
        this.operationEnergyCost = 0.0;
        this.worldInteractorEnergyCost = 0.0;
        this.executedOperationCount = 0;
        this.runCount = 0;
    }

    // ========== Getters ==========

    /**
     * 获取运算核心消耗的能量
     */
    public double getCoreEnergyCost() {
        return coreEnergyCost;
    }

    /**
     * 获取基础操作消耗的能量（操作本身的 cost）
     */
    public double getOperationEnergyCost() {
        return operationEnergyCost;
    }

    /**
     * 获取世界交互器额外消耗的能量
     */
    public double getWorldInteractorEnergyCost() {
        return worldInteractorEnergyCost;
    }

    /**
     * 获取执行的操作数量
     */
    public int getExecutedOperationCount() {
        return executedOperationCount;
    }

    /**
     * 获取经历的 tick 次数
     */
    public int getRunCount() {
        return runCount;
    }

    /**
     * 格式化为面板显示
     */
    public String[] formatStatsPanel() {
        return new String[] {
                "§7Tick 次数：§f" + runCount,
                "§7执行操作：§f" + executedOperationCount,
                "§7能量消耗:",
                "  §7核心：§f" + String.format("%.1f", coreEnergyCost),
                "  §7操作：§f" + String.format("%.1f", operationEnergyCost),
                "  §7世界交互器：§f" + String.format("%.1f", worldInteractorEnergyCost),
        };
    }
}
