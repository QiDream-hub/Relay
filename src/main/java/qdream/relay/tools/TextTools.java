package qdream.relay.tools;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Instruction;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/**
 * 文本工具类 - 提供 ID 获取和文本翻译功能
 *
 * <p>
 * 提供以下功能：
 * </p>
 * <ul>
 * <li>获取可执行单元的 ID 和翻译键</li>
 * <li>获取翻译后的文本（支持参数化消息）</li>
 * <li>格式化栈快照为字符串</li>
 * </ul>
 *
 * <h2>翻译键规范</h2>
 * <p>
 * 所有翻译键遵循统一的命名规范：
 * </p>
 * <ul>
 * <li>操作名称：{@code operation.relay:<id>.name}</li>
 * <li>操作描述：{@code operation.relay:<id>.description}</li>
 * <li>类型名称：{@code type.relay:<id>.name}</li>
 * <li>参数描述：{@code param.<name>.description}</li>
 * <li>错误消息：{@code error.relay:<type>} (待实现)</li>
 * </ul>
 *
 */
public final class TextTools {

    private TextTools() {
        // 防止实例化
    }

    // ==================== 获取 ID 系列 ====================

    /**
     * 获取可执行单元的 ID
     *
     * @param exe 可执行单元
     * @return ID 或类名
     */
    public static String getId(Executable exe) {
        if (exe instanceof Data data) {
            return data.getId();
        } else if (exe instanceof Instruction instr) {
            return instr.getId();
        } else {
            return exe.getClass().getSimpleName();
        }
    }

    /**
     * 获取 NBT 标签中可执行单元的 ID
     *
     * @param tag NBT 标签
     * @return ID 或 "unknown"
     */
    public static String getId(CompoundTag tag) {
        Optional<String> string = tag.getString("id");
        if (string.isEmpty()) {
            return "unknown";
        }
        Optional<Executable> optional = OperationRegistry.get(string.get());
        if (optional.isEmpty()) {
            return "unknown";
        }
        Executable exe = optional.get();
        if (exe instanceof Data data) {
            return data.getId();
        } else if (exe instanceof Instruction instr) {
            return instr.getId();
        } else {
            return exe.getClass().getSimpleName();
        }
    }

    // ==================== 基础翻译方法 ====================

    /**
     * 获取翻译键对应的文本组件（带参数）
     *
     * @param key  翻译键
     * @param args 参数列表（支持 Component 或 Object）
     * @return Component 翻译后的文本组件
     */
    public static Component getComponent(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /**
     * 获取翻译键对应的文本（带参数）
     *
     * @param key  翻译键
     * @param args 参数列表
     * @return 翻译后的文本
     */
    public static String getText(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    // ==================== 翻译键获取 ====================
    @NonNull
    public static String geParamNameKey(Executable exe) {
        return "param.shell.description";
    }

    @NonNull
    public static String geDescriptiontKey(Executable exe) {
        if (exe instanceof Data data) {
            return "type." + data.getId() + ".description";
        } else if (exe instanceof Instruction instr) {
            return "operation." + instr.getId() + ".description";
        }
        return "unknown.name";
    }

    /**
     * 获取可执行单元的翻译键
     *
     * @param exe 可执行单元
     * @return 翻译键
     */
    @NonNull
    public static String getNameKey(Executable exe) {
        if (exe instanceof Data data) {
            return "type." + data.getId() + ".name";
        } else if (exe instanceof Instruction instr) {
            return "operation." + instr.getId() + ".name";
        }
        return "unknown.name";
    }

    /**
     * 通过 ID 获取可执行单元的翻译键
     *
     * @param id 可执行单元的 ID（如 "relay:number", "relay:add"）
     * @return 翻译键（如 "type.relay:number.name", "operation.relay:add.name"），如果 ID
     *         无效则返回 "unknown.name"
     */
    @NonNull
    public static String getNameKey(String id) {
        if (id == null || id.isEmpty()) {
            return "unknown.name";
        }
        Optional<Executable> optional = OperationRegistry.get(id);
        if (optional.isEmpty()) {
            return "unknown.name";
        }
        Executable exe = optional.get();
        if (exe instanceof Data data) {
            return "type." + data.getId() + ".name";
        } else if (exe instanceof Instruction instr) {
            return "operation." + instr.getId() + ".name";
        }
        return "unknown.name";
    }

    /**
     * 获取 NBT 标签中可执行单元的翻译键
     *
     * @param tag NBT 标签
     * @return 翻译键
     */
    @NonNull
    public static String getNameKey(CompoundTag tag) {
        Optional<String> string = tag.getString("id");
        if (string.isEmpty()) {
            return "unknown.name";
        }
        Executable ex = OperationRegistry.get(string.get()).orElseGet(null);
        if (ex instanceof Instruction ins) {
            return "operation." + ins.getId() + ".name";
        } else if (ex instanceof Data data) {
            return "type." + data.getId() + ".name";
        }
        return "unknown.name";
    }

    // ==================== 翻译方法 ====================

    /**
     * 获取可执行单元的显示名称（从翻译键）
     *
     * @param exe 可执行单元
     * @return 显示名称
     */
    public static String getName(Executable exe) {
        return getText(getNameKey(exe));
    }

    /**
     * 获取 NBT 标签中可执行单元的显示名称（从翻译键）
     *
     * @param tag NBT 标签
     * @return 显示名称
     */
    public static String getName(CompoundTag tag) {
        return getText(getNameKey(tag));
    }

    /**
     * 获取翻译键对应的文本（字符串形式）
     *
     * @param id 操作id
     * @return 翻译后的文本
     */
    public static String getName(String id) {
        return getText(getNameKey(id));
    }

    /**
     * 翻译类型(包括any与...any)
     *
     * @param id 操作id
     * @return 翻译后的文本
     */
    public static String getTypeName(String id) {
        if (id.equals("any") || id.equals("...any")) {
            return getText("type." + id + ".name");
        }
        return getText(getNameKey(id));
    }

    // ==================== 描述翻译 ====================
    /**
     * 获取可执行单元的描述文本
     *
     * @param exe 可执行单元
     * @return 描述文本
     */
    public static String getDescriptionText(Executable exe) {
        return getText(getDescriptionKey(exe));
    }

    /**
     * 获取可执行单元的描述翻译键
     *
     * @param exe 可执行单元
     * @return 翻译键
     */
    @NonNull
    public static String getDescriptionKey(Executable exe) {
        if (exe instanceof Data data) {
            return "type." + data.getId() + ".description";
        } else if (exe instanceof Instruction instr) {
            return "operation." + instr.getId() + ".description";
        }
        return "unknown.description";
    }

    // ==================== 参数名翻译 ====================
    public static String getParamNameText(String name) {
        return getText("param." + name + ".description");
    }

    // ==================== 栈格式化 ====================

    /**
     * 格式化数据栈快照为字符串
     *
     * @param executor 状态机
     * @return 格式化后的字符串
     */
    public static String formatDataStack(StateMachine executor) {
        return formatStack(executor.getDataStackSnapshot());
    }

    /**
     * 格式化程序栈快照为字符串
     *
     * @param executor 状态机
     * @return 格式化后的字符串
     */
    public static String formatProgramStack(StateMachine executor) {
        return formatStack(executor.getProgramStackSnapshot());
    }

    /**
     * 格式化栈快照为字符串
     *
     * @param stack 栈快照
     * @return 格式化后的字符串
     */
    public static String formatStack(List<Executable> stack) {
        if (stack.isEmpty()) {
            return "§8[]";
        }
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                sb.append("§8, ");
            }
            Executable exe = stack.get(i);
            if (exe instanceof Instruction instr) {
                sb.append("§e").append(getName(instr.getId()));
            } else if (exe instanceof Data data) {
                sb.append("§e").append(data.asString());
            } else {
                sb.append("§f").append(exe.getClass().getSimpleName());
            }
        }
        sb.append("§8]");
        return sb.toString();
    }
}
