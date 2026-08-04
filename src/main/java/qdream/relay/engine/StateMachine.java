package qdream.relay.engine;

import qdream.relay.mc.errors.ExecutionException;

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
     * 捕获 ExecutionException 并触发 mishap
     */
    public void step() {
        if (programStack.isEmpty()) {
            throw new Warning(this, "已运行完成");
        }

        Executable executable = programStack.pop();
        try {
            executable.execute(this);
        } catch (ExecutionException e) {
            triggerMishap(e.getMessage());
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
     * 
     * @param key   键
     * @param value 值（可以是任意对象，如 ItemStack、ServerLevel 等）
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * 获取上下文数据
     * 
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    public Object getContext(String key) {
        return context.get(key);
    }

    /**
     * 获取上下文数据（类型安全版本）
     * 
     * @param key  键
     * @param type 期望的类型
     * @return Optional<值>
     */
    public <T> Optional<T> getContext(String key, Class<T> type) {
        return Optional.ofNullable(context.get(key))
                .map(type::cast);
    }

    /**
     * 检查是否存在上下文数据
     * 
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

    // ========== 索引访问操作 ==========

    /**
     * 获取程序栈指定索引位置的元素
     * 
     * @param index 索引（0 为栈顶）
     * @return 元素，索引越界返回 null
     */
    public Executable getProgramAt(int index) {
        if (index < 0 || index >= programStack.size()) {
            return null;
        }
        List<Executable> snapshot = new ArrayList<>(programStack);
        return snapshot.get(index);
    }

    /**
     * 设置程序栈指定索引位置的元素
     * 
     * @param index      索引（0 为栈顶）
     * @param executable 新元素
     * @return 是否成功（索引越界返回 false）
     */
    public boolean setProgramAt(int index, Executable executable) {
        if (index < 0 || index >= programStack.size()) {
            return false;
        }
        List<Executable> snapshot = new ArrayList<>(programStack);
        snapshot.set(index, executable);
        programStack.clear();
        programStack.addAll(snapshot);
        return true;
    }

    /**
     * 移除程序栈指定索引位置的元素
     * 
     * @param index 索引（0 为栈顶）
     * @return 被移除的元素，索引越界返回 null
     */
    public Executable removeProgramAt(int index) {
        if (index < 0 || index >= programStack.size()) {
            return null;
        }
        List<Executable> snapshot = new ArrayList<>(programStack);
        Executable removed = snapshot.remove(index);
        programStack.clear();
        programStack.addAll(snapshot);
        return removed;
    }

    /**
     * 获取数据栈指定索引位置的元素
     * 
     * @param index 索引（0 为栈顶）
     * @return 元素，索引越界返回 null
     */
    public Executable getDataAt(int index) {
        if (index < 0 || index >= dataStack.size()) {
            return null;
        }
        List<Executable> snapshot = new ArrayList<>(dataStack);
        return snapshot.get(index);
    }

    /**
     * 设置数据栈指定索引位置的元素
     * 
     * @param index      索引（0 为栈顶）
     * @param executable 新元素
     * @return 是否成功（索引越界返回 false）
     */
    public boolean setDataAt(int index, Executable executable) {
        if (index < 0 || index >= dataStack.size()) {
            return false;
        }
        List<Executable> snapshot = new ArrayList<>(dataStack);
        snapshot.set(index, executable);
        dataStack.clear();
        dataStack.addAll(snapshot);
        return true;
    }

    /**
     * 移除数据栈指定索引位置的元素
     * 
     * @param index 索引（0 为栈顶）
     * @return 被移除的元素，索引越界返回 null
     */
    public Executable removeDataAt(int index) {
        if (index < 0 || index >= dataStack.size()) {
            return null;
        }
        List<Executable> snapshot = new ArrayList<>(dataStack);
        Executable removed = snapshot.remove(index);
        dataStack.clear();
        dataStack.addAll(snapshot);
        return removed;
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
