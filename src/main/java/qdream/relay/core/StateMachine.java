package qdream.relay.core;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 状态机执行器
 * 维护双栈，执行操作，处理持久化
 */
public class StateMachine {
    private final Deque<Iota> programStack = new ArrayDeque<>();
    private final Deque<Iota> dataStack = new ArrayDeque<>();
    
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
     * 从磁盘加载程序
     * 程序列表需要反转后压入程序栈，保证从左到右的执行顺序
     */
    public void loadProgram(List<Iota> program) {
        List<Iota> reversed = new ArrayList<>(program);
        Collections.reverse(reversed);
        programStack.clear();
        for (Iota iota : reversed) {
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
            Iota top = programStack.pop();
            
            if (top.isString()) {
                // 操作 ID
                String opId = top.asString();
                executeOperation(opId);
            } else if (top.isList()) {
                // 列表 - 反转后压入程序栈
                List<Iota> list = top.asList();
                List<Iota> reversed = new ArrayList<>(list);
                Collections.reverse(reversed);
                for (Iota iota : reversed) {
                    programStack.push(iota);
                }
                remainingOps--;
            } else if (top.isNull() || top.isNumber() || top.isBoolean() || 
                       top.isVector() || top.isString() || top.isEntity()) {
                // 数据 - 自动压入数据栈（宽容规则）
                dataStack.push(top);
                remainingOps--;
            } else {
                triggerMishap("未知的栈顶类型：" + top.getType());
            }
        }
    }

    /**
     * 执行操作
     */
    private void executeOperation(String opId) {
        OperationRegistry.getEntry(opId).ifPresentOrElse(entry -> {
            // 检查世界交互器
            if (entry.requiresWorldInteractor() && !hasWorldInteractor) {
                triggerMishap("操作 " + opId + " 需要世界交互器");
                return;
            }

            // 检查操作数
            if (remainingOps < entry.getCost()) {
                triggerMishap("操作 " + opId + " 需要 " + entry.getCost() + " 操作数，但只剩 " + remainingOps);
                return;
            }

            try {
                entry.getOperation().execute(this);
                remainingOps -= entry.getCost();
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
    public Iota popProgram() {
        return programStack.pollFirst();
    }

    /**
     * 压入程序栈
     */
    public void pushProgram(Iota iota) {
        if (programStack.size() >= maxStackSize) {
            triggerMishap("程序栈超出大小限制 (" + maxStackSize + ")");
            return;
        }
        programStack.push(iota);
    }

    /**
     * 从数据栈弹出
     */
    public Iota popData() {
        if (dataStack.isEmpty()) {
            triggerMishap("数据栈为空，无法弹出");
            return null;
        }
        return dataStack.pop();
    }

    /**
     * 查看数据栈顶部
     */
    public Iota peekData() {
        if (dataStack.isEmpty()) {
            return null;
        }
        return dataStack.peek();
    }

    /**
     * 压入数据栈
     */
    public void pushData(Iota iota) {
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

    // ========== NBT 持久化 ==========

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        ListTag programList = new ListTag();
        for (Iota iota : programStack) {
            programList.add(iota.toNbt());
        }
        tag.put("programStack", programList);

        ListTag dataList = new ListTag();
        for (Iota iota : dataStack) {
            dataList.add(iota.toNbt());
        }
        tag.put("dataStack", dataList);

        tag.putBoolean("hasWorldInteractor", hasWorldInteractor);
        tag.putInt("maxStackSize", maxStackSize);

        return tag;
    }

    public void fromNbt(CompoundTag tag) {
        programStack.clear();
        dataStack.clear();

        ListTag programList = tag.getList("programStack").orElse(new ListTag());
        for (Tag element : programList) {
            programStack.push(Iota.fromNbt((CompoundTag) element));
        }

        ListTag dataList = tag.getList("dataStack").orElse(new ListTag());
        for (Tag element : dataList) {
            dataStack.push(Iota.fromNbt((CompoundTag) element));
        }

        hasWorldInteractor = tag.getBoolean("hasWorldInteractor").orElse(false);
        maxStackSize = tag.getInt("maxStackSize").orElse(1024);
    }

    // ========== 调试 ==========

    /**
     * 获取数据栈快照（用于调试）
     */
    public List<Iota> getDataStackSnapshot() {
        return new ArrayList<>(dataStack);
    }

    /**
     * 获取程序栈快照（用于调试）
     */
    public List<Iota> getProgramStackSnapshot() {
        return new ArrayList<>(programStack);
    }
}
