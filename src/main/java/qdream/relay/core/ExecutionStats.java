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

    /** 操作消耗的能量（世界交互器额外消耗） */
    private double operationEnergyCost;

    /** 实际执行的操作数量 */
    private int executedOperationCount;

    /** 程序运行次数（从启动到停止的次数） */
    private int runCount;

    public ExecutionStats() {
        this.coreEnergyCost = 0.0;
        this.operationEnergyCost = 0.0;
        this.executedOperationCount = 0;
        this.runCount = 0;
    }

    /**
     * 添加能量消耗
     *
     * @param coreCost      核心基础消耗
     * @param operationCost 操作额外消耗（世界交互器等）
     */
    public void addEnergyCost(double coreCost, double operationCost) {
        this.coreEnergyCost += coreCost;
        this.operationEnergyCost += operationCost;
    }

    /**
     * 递增程序运行次数
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
     * 重置统计信息（程序停止时调用）
     */
    public void reset() {
        this.coreEnergyCost = 0.0;
        this.operationEnergyCost = 0.0;
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
     * 获取操作消耗的能量（世界交互器额外消耗）
     */
    public double getOperationEnergyCost() {
        return operationEnergyCost;
    }

    /**
     * 获取总能量消耗
     */
    public double getTotalEnergyCost() {
        return coreEnergyCost + operationEnergyCost;
    }

    /**
     * 获取执行的操作数量
     */
    public int getExecutedOperationCount() {
        return executedOperationCount;
    }

    /**
     * 获取程序运行次数
     */
    public int getRunCount() {
        return runCount;
    }

    /**
     * 格式化为面板显示
     */
    public String[] formatStatsPanel() {
        return new String[] {
                "§7运行次数：§f" + runCount,
                "§7执行操作：§f" + executedOperationCount,
                "§7能量消耗:",
                "  §7核心：§f" + String.format("%.1f", coreEnergyCost),
                "  §7操作：§f" + String.format("%.1f", operationEnergyCost),
                "  §7总计：§f" + String.format("%.1f", getTotalEnergyCost()),
        };
    }
}
