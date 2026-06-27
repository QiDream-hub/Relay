package qdream.relay.engine;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 状态机执行器
 * 维护双栈，执行操作
 * engine 层保持最小化，只负责纯粹的栈管理和原子执行
 */
public class StateMachine {
    private final Deque<Executable> programStack = new ArrayDeque<>();
    private final Deque<Executable> dataStack = new ArrayDeque<>();
    private final Map<String, Object> context = new HashMap<>();

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
    }

    // ========== 程序加载 ==========

    /**
     * 从列表加载程序
     * 程序列表需要反转后压入程序栈，保证从左到右的执行顺序
     */
    public void loadProgram(List<Executable> program) {
        List<Executable> reversed = new ArrayList<>(program);
        Collections.reverse(reversed);
        for (Executable iota : reversed) {
            pushProgram(iota);
        }
    }

    // ========== 执行 ==========

    /**
     * 执行栈顶单个操作
     * <p>
     * engine 层保持最小化，只负责原子执行。
     *
     * @return true 成功执行，false 需要中断（栈空或执行失败）
     */
    public boolean step() {
        if (programStack.isEmpty()) {
            return false;
        }

        Executable executable = programStack.pop();
        try {
            executable.execute(this);
            return true;
        } catch (Exception e) {
            triggerMishap("未知错误:" + e.getMessage());
            return false;
        }
    }

    // ========== 事故处理 ==========

    /**
     * 触发事故
     * 清空双栈并终止执行
     */
    public void triggerMishap(String reason) {
        programStack.clear();
        dataStack.clear();

        if (mishapHandler != null) {
            mishapHandler.onMishap(reason);
        }
    }

    /**
     * 清空状态机（用于重置）
     */
    public void clear() {
        programStack.clear();
        dataStack.clear();
    }

    public void setMishapHandler(MishapHandler handler) {
        this.mishapHandler = handler;
    }

    // ========== 上下文管理 ==========

    /**
     * 设置上下文数据
     * @param key 键
     * @param value 值（可以是任意对象，如 ItemStack、ServerLevel 等）
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * 获取上下文数据
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    public Object getContext(String key) {
        return context.get(key);
    }

    /**
     * 获取上下文数据（类型安全版本）
     * @param key 键
     * @param type 期望的类型
     * @return Optional<值>
     */
    public <T> Optional<T> getContext(String key, Class<T> type) {
    return Optional.ofNullable(context.get(key))
                   .map(type::cast);
}

    /**
     * 检查是否存在上下文数据
     * @param key 键
     * @return 是否存在
     */
    public boolean hasContext(String key) {
        return context.containsKey(key);
    }

    /**
     * 清空上下文
     */
    public void clearContext() {
        context.clear();
    }

    // ========== 栈操作 ==========

    /**
     * 从程序栈弹出
     */
    public Executable popProgram() {
        return programStack.pollFirst();
    }

    /**
     * 查看程序栈顶部（不弹出）
     */
    public Executable peekProgram() {
        return programStack.peekFirst();
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

    /**
     * 是否正在运行（程序栈非空）
     */
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
