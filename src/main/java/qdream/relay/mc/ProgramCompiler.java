package qdream.relay.mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import qdream.relay.types.*;
import qdream.relay.engine.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 法术程序编译器
 * 将文本格式的程序字符串编译为 Executable 列表
 * 
 * 支持的语法：
 * - 数据字面量：number(1), bool(true), str("hello"), vec(1,2,3), entity(uuid), list(...), null
 * - 操作指令：直接使用操作 ID，如 relay:add
 * - 分隔符：分号 ;
 * 
 * 示例：
 * number(1);number(2);relay:add
 * list(number(1);str("hello");relay:pop)
 */
public class ProgramCompiler {

    private final String input;
    private int pos;

    public ProgramCompiler(String input) {
        this.input = input.trim();
        this.pos = 0;
    }

    /**
     * 编译程序字符串
     * @param programStr 程序字符串
     * @return 编译后的 Executable 列表
     * @throws CompilationException 编译错误
     */
    public static List<Executable> compile(String programStr) throws CompilationException {
        if (programStr == null || programStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 去除首尾的单引号（Minecraft 命令中单引号不会被自动解析）
        programStr = programStr.trim();
        if (programStr.startsWith("'") && programStr.endsWith("'")) {
            programStr = programStr.substring(1, programStr.length() - 1);
        }

        ProgramCompiler compiler = new ProgramCompiler(programStr);
        return compiler.compileProgram();
    }

    /**
     * 编译程序（入口）
     */
    private List<Executable> compileProgram() throws CompilationException {
        List<Executable> program = new ArrayList<>();

        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) break;

            Executable instr = parseInstruction();
            if (instr != null) {
                program.add(instr);
            }

            // 跳过可选的分号
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ';') {
                pos++;
            }
        }

        return program;
    }

    /**
     * 解析单个指令（数据或操作）
     */
    private Executable parseInstruction() throws CompilationException {
        skipWhitespace();
        if (pos >= input.length()) return null;

        // 尝试解析为数据字面量
        if (input.startsWith("number(", pos)) {
            return parseNumber();
        }
        if (input.startsWith("bool(", pos)) {
            return parseBoolean();
        }
        if (input.startsWith("str(", pos)) {
            return parseString();
        }
        if (input.startsWith("vec(", pos)) {
            return parseVector();
        }
        if (input.startsWith("entity(", pos)) {
            return parseEntity();
        }
        if (input.startsWith("list(", pos)) {
            return parseList();
        }
        if (input.startsWith("null", pos) && (pos + 4 >= input.length() || !Character.isLetterOrDigit(input.charAt(pos + 4)))) {
            pos += 4;
            return NullIota.INSTANCE;
        }

        // 尝试解析为操作 ID
        return parseOperation();
    }

    /**
     * 解析数字
     */
    private NumberIota parseNumber() throws CompilationException {
        pos += 7; // 跳过 "number("
        skipWhitespace();

        int start = pos;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.' || input.charAt(pos) == '-')) {
            pos++;
        }

        String value = input.substring(start, pos);
        expect(')');

        try {
            return new NumberIota(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            throw new CompilationException("无效的数字格式：" + value);
        }
    }

    /**
     * 解析布尔值
     */
    private BooleanIota parseBoolean() throws CompilationException {
        pos += 5; // 跳过 "bool("
        skipWhitespace();

        int start = pos;
        while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
            pos++;
        }

        String value = input.substring(start, pos);
        expect(')');

        return new BooleanIota(Boolean.parseBoolean(value));
    }

    /**
     * 解析字符串
     */
    private StringIota parseString() throws CompilationException {
        pos += 4; // 跳过 "str("
        skipWhitespace();

        if (pos >= input.length() || input.charAt(pos) != '"') {
            throw new CompilationException("字符串必须以 \" 开头");
        }
        pos++; // 跳过开头的 "

        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != '"') {
            if (input.charAt(pos) == '\\') {
                pos++;
                if (pos >= input.length()) {
                    throw new CompilationException("未闭合的转义字符");
                }
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(escaped); break;
                }
            } else {
                sb.append(input.charAt(pos));
            }
            pos++;
        }

        expect('"');
        expect(')');

        return new StringIota(sb.toString());
    }

    /**
     * 解析向量
     */
    private VectorIota parseVector() throws CompilationException {
        pos += 4; // 跳过 "vec("
        skipWhitespace();

        double x = parseDouble();
        expectComma();
        double y = parseDouble();
        expectComma();
        double z = parseDouble();

        skipWhitespace();
        expect(')');

        return new VectorIota(new net.minecraft.world.phys.Vec3(x, y, z));
    }

    /**
     * 解析实体引用
     */
    private EntityIota parseEntity() throws CompilationException {
        pos += 7; // 跳过 "entity("
        skipWhitespace();

        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '-')) {
            pos++;
        }

        String uuidStr = input.substring(start, pos);
        expect(')');

        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
            return new EntityIota(uuid);
        } catch (IllegalArgumentException e) {
            throw new CompilationException("无效的 UUID 格式：" + uuidStr);
        }
    }

    /**
     * 解析列表（支持嵌套）
     */
    private ListIota parseList() throws CompilationException {
        pos += 5; // 跳过 "list("
        skipWhitespace();

        // 空列表
        if (pos < input.length() && input.charAt(pos) == ')') {
            pos++;
            return new ListIota(new ArrayList<>());
        }

        // 递归编译列表内容
        List<Executable> items = new ArrayList<>();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new CompilationException("未闭合的列表");
            }

            // 检查列表结束
            if (input.charAt(pos) == ')') {
                pos++;
                break;
            }

            // 解析列表元素（递归调用）
            Executable item = parseInstruction();
            if (item != null) {
                items.add(item);
            }

            // 跳过可选的分号
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ';') {
                pos++;
            }
        }

        return new ListIota(items);
    }

    /**
     * 解析操作 ID
     */
    private Executable parseOperation() throws CompilationException {
        skipWhitespace();
        int start = pos;

        // 操作 ID 可以包含字母、数字、下划线、连字符、冒号、斜杠
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ':' || c == '/') {
                pos++;
            } else {
                break;
            }
        }

        if (start == pos) {
            throw new CompilationException("无法解析操作 ID，位置：" + pos);
        }

        String opId = input.substring(start, pos);

        Optional<Executable> op = OperationRegistry.get(opId);
        if (op.isEmpty()) {
            throw new CompilationException("未知的操作：" + opId);
        }

        return op.get();
    }

    /**
     * 解析双精度数
     */
    private double parseDouble() throws CompilationException {
        skipWhitespace();
        int start = pos;

        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.' || input.charAt(pos) == '-')) {
            pos++;
        }

        String value = input.substring(start, pos).trim();
        if (value.isEmpty()) {
            throw new CompilationException("期望数字，实际为空");
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new CompilationException("无效的数字格式：" + value);
        }
    }

    /**
     * 期望下一个字符是逗号
     */
    private void expectComma() throws CompilationException {
        skipWhitespace();
        if (pos >= input.length() || input.charAt(pos) != ',') {
            throw new CompilationException("期望 ','，实际 '" + (pos < input.length() ? input.charAt(pos) : "EOF") + "'");
        }
        pos++;
    }

    /**
     * 期望下一个字符是指定字符
     */
    private void expect(char c) throws CompilationException {
        skipWhitespace();
        if (pos >= input.length() || input.charAt(pos) != c) {
            throw new CompilationException("期望 '" + c + "'，实际 '" + (pos < input.length() ? input.charAt(pos) : "EOF") + "'");
        }
        pos++;
    }

    /**
     * 跳过空白字符
     */
    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    /**
     * 将程序列表编译为 JSON 数组
     * @param program 程序列表
     * @return JSON 数组
     */
    public static JsonArray toJson(List<Executable> program) {
        JsonArray array = new JsonArray();
        for (Executable exec : program) {
            OperationRegistry.serializeToJson(exec).ifPresent(array::add);
        }
        return array;
    }

    /**
     * 将程序列表编译为 JSON 字符串
     * @param program 程序列表
     * @return JSON 字符串
     */
    public static String toJsonString(List<Executable> program) {
        return toJson(program).toString();
    }

    /**
     * 从 JSON 字符串反编译为程序列表
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
            Optional<Executable> exec = OperationRegistry.deserializeFromJson(obj);
            if (exec.isEmpty()) {
                String id = obj.has("id") ? obj.get("id").getAsString() : "未知";
                throw new CompilationException("未知的指令: " + id + " (位置 #" + i + ")");
            }
            program.add(exec.get());
        }
        return program;
    }

    /**
     * 编译异常
     */
    public static class CompilationException extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }
}
