package qdream.relay.engine;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 状态机执行器
 * 维护双栈，执行操作
 *
 * 纯 Java 实现，不依赖 Minecraft
 */
public class StateMachine {
    private final Deque<Executable> programStack = new ArrayDeque<>();
    private final Deque<Executable> dataStack = new ArrayDeque<>();

    private int remainingOps;
    private boolean hasWorldInteractor;
    private int maxStackSize;

    /**
     * 事故回调
     */
    public interface MishapHandler {
        void onMishap(String reason);
    }

    private MishapHandler mishapHandler;

    public StateMachine() {
        this(1024);
    }

    public StateMachine(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        this.remainingOps = 0;
        this.hasWorldInteractor = false;
    }

    // ========== 程序加载 ==========

    /**
     * 从列表加载程序
     * 程序列表需要反转后压入程序栈，保证从左到右的执行顺序
     */
    public void loadProgram(List<Executable> program) {
        List<Executable> reversed = new ArrayList<>(program);
        Collections.reverse(reversed);
        programStack.clear();
        for (Executable iota : reversed) {
            programStack.push(iota);
        }
    }

    // ========== Tick 执行 ==========

    /**
     * 执行一个 tick
     * @param ops 本 tick 可用的操作数
     */
    public void tick(int ops) {
        remainingOps = ops;

        while (remainingOps > 0 && !programStack.isEmpty()) {
            Executable executable = programStack.pop();
            executable.execute(this);
            remainingOps--;
        }
    }

    /**
     * 执行操作
     * @param opId 操作 ID
     */
    public void executeOperation(String opId) {
        OperationRegistry.get(opId).ifPresentOrElse(op -> {
            // 检查操作数预算
            if (remainingOps < op.getCost()) {
                triggerMishap("操作 " + opId + " 需要 " + op.getCost() + " 操作数，但只剩 " + remainingOps);
                return;
            }

            try {
                op.execute(this);
                remainingOps -= op.getCost();
            } catch (Exception e) {
                triggerMishap("执行操作 " + opId + " 时出错：" + e.getMessage());
            }
        }, () -> triggerMishap("未知操作：" + opId));
    }

    // ========== 事故处理 ==========

    /**
     * 触发事故
     * 清空双栈并终止执行
     */
    public void triggerMishap(String reason) {
        programStack.clear();
        dataStack.clear();
        remainingOps = 0;

        if (mishapHandler != null) {
            mishapHandler.onMishap(reason);
        }
    }

    public void setMishapHandler(MishapHandler handler) {
        this.mishapHandler = handler;
    }

    // ========== 栈操作 ==========

    /**
     * 从程序栈弹出
     */
    public Executable popProgram() {
        return programStack.pollFirst();
    }

    /**
     * 压入程序栈
     */
    public void pushProgram(Executable iota) {
        if (programStack.size() >= maxStackSize) {
            triggerMishap("程序栈超出大小限制 (" + maxStackSize + ")");
            return;
        }
        programStack.push(iota);
    }

    /**
     * 从数据栈弹出
     */
    public Executable popData() {
        if (dataStack.isEmpty()) {
            triggerMishap("数据栈为空，无法弹出");
            return null;
        }
        return dataStack.pop();
    }

    /**
     * 查看数据栈顶部
     */
    public Executable peekData() {
        if (dataStack.isEmpty()) {
            return null;
        }
        return dataStack.peek();
    }

    /**
     * 压入数据栈
     */
    public void pushData(Executable iota) {
        if (dataStack.size() >= maxStackSize) {
            triggerMishap("数据栈超出大小限制 (" + maxStackSize + ")");
            return;
        }
        dataStack.push(iota);
    }

    /**
     * 获取数据栈大小
     */
    public int getDataStackSize() {
        return dataStack.size();
    }

    /**
     * 获取程序栈大小
     */
    public int getProgramStackSize() {
        return programStack.size();
    }

    // ========== 状态 ==========

    public void setHasWorldInteractor(boolean has) {
        this.hasWorldInteractor = has;
    }

    public boolean hasWorldInteractor() {
        return hasWorldInteractor;
    }

    public int getRemainingOps() {
        return remainingOps;
    }

    public boolean isRunning() {
        return !programStack.isEmpty();
    }

    public void setMaxStackSize(int size) {
        this.maxStackSize = size;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    // ========== 调试 ==========

    /**
     * 获取数据栈快照（用于调试）
     */
    public List<Executable> getDataStackSnapshot() {
        return new ArrayList<>(dataStack);
    }

    /**
     * 获取程序栈快照（用于调试）
     */
    public List<Executable> getProgramStackSnapshot() {
        return new ArrayList<>(programStack);
    }
}
