package qdream.relay.mc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 法术程序编译器
 * 负责 JSON 格式的程序序列化与反序列化
 * <p>
 * 程序格式：JSON 数组，每个元素包含 "id" 字段标识类型
 * 
 * <pre>
 * [{"id":"relay:number","value":42},{"id":"relay:number","value":42},{"id":"relay:add"}]
 * </pre>
 */
public class ProgramCompiler {

    private ProgramCompiler() {
    }

    // ========== 序列化 ==========

    /**
     * 将程序列表编译为 JSON 数组
     * 
     * @param program 程序列表
     * @return JSON 数组
     */
    public static JsonArray toJson(List<Executable> program) {
        JsonArray array = new JsonArray();
        for (Executable exec : program) {
            JsonObject json = new JsonObject();
            ((Operation) exec).toJson(json);
            array.add(json);
        }
        return array;
    }

    /**
     * 将程序列表编译为 JSON 字符串
     * 
     * @param program 程序列表
     * @return JSON 字符串
     */
    public static String toJsonString(List<Executable> program) {
        return toJson(program).toString();
    }

    /**
     * 将程序列表编译为格式化的 JSON 字符串
     * 
     * @param program 程序列表
     * @return 格式化 JSON 字符串
     */
    public static String toPrettyJson(List<Executable> program) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();
        for (Executable item : program) {
            if (item instanceof Operation operation) {
                JsonObject op = new JsonObject();
                operation.toJson(op);
                array.add(op);
            }
        }
        return gson.toJson(array);
    }

    // ========== 反序列化 ==========

    /**
     * 从 JSON 字符串反编译为程序列表
     * 
     * @param jsonStr JSON 字符串
     * @return 程序列表
     * @throws CompilationException 解析错误
     */
    public static List<Executable> compileFromJson(String jsonStr) throws CompilationException {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JsonElement element = JsonParser.parseString(jsonStr);
            if (!element.isJsonArray()) {
                throw new CompilationException("JSON 程序必须是数组格式");
            }
            return fromJson(element.getAsJsonArray());
        } catch (com.google.gson.JsonSyntaxException e) {
            throw new CompilationException("JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 从 JSON 数组反编译为程序列表
     * 
     * @param array JSON 数组
     * @return 程序列表
     * @throws CompilationException 解析错误
     */
    public static List<Executable> fromJson(JsonArray array) throws CompilationException {
        List<Executable> program = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                throw new CompilationException("程序元素 #" + i + " 必须是 JSON 对象");
            }
            JsonObject obj = element.getAsJsonObject();
            String id = obj.has("id") ? obj.get("id").getAsString() : "";
            Optional<OperationRegistry.Entry> entryOpt = OperationRegistry.getEntry(id);
            if (entryOpt.isEmpty()) {
                throw new CompilationException("未知的指令: " + id + " (位置 #" + i + ")");
            }
            Operation instance = (Operation) entryOpt.get().create();
            program.add(instance.fromJson(obj));
        }
        return program;
    }

    public static List<Executable> fromNbt(ListTag nbt) throws CompilationException {
        List<Executable> program = new ArrayList<>();
        for (int i = 0; i < nbt.size(); i++) {
            CompoundTag tag = nbt.getCompound(i).orElseThrow(() -> new CompilationException("NBT 缺少指令数据"));
            String id = tag.getString("id").orElseThrow(() -> new CompilationException("NBT 缺少指令 ID"));
            Optional<OperationRegistry.Entry> entryOpt = OperationRegistry.getEntry(id);
            if (entryOpt.isEmpty()) {
                throw new CompilationException("未知的指令: " + id + " (位置 #" + i + ")");
            }
            Operation instance = (Operation) entryOpt.get().create();
            program.add(instance.fromNbt(tag));
        }
        return program;
    }

    public static ListTag toNbt(List<Executable> program) throws CompilationException {
        ListTag listTag = new ListTag();
        for (Executable exec : program) {
            if (exec instanceof Operation op) {
                CompoundTag tag = new CompoundTag();
                op.toNbt(tag);
                listTag.add(tag);
            } else {
                throw new CompilationException("指令 " + exec + " 不是 Operation 类型");
            }
        }
        return listTag;
    }

    // ========== 异常 ==========

    /**
     * 编译异常
     */
    public static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }
}
