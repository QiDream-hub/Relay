package qdream.relay.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.RelayOperations;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.mc.signature.ParameterDescriptor;

/**
 * 操作语言模板生成 / 完整性检查工具
 *
 * <p>自动根据操作注册表（{@link OperationRegistry}）中每个操作的签名，生成操作语言文件的公共模板，
 * 并检查指定语言文件是否已包含该操作的全部翻译键：
 *
 * <pre>
 * "operation.relay:pickup_item.name": "",
 * "operation.relay:pickup_item.description": "",
 * "operation.relay:pickup_item.param.container.description": "",
 * "operation.relay:pickup_item.param.entity.description": "",
 * "operation.relay:pickup_item.param.slots.description": "",
 * </pre>
 *
 * <p>每次新增操作后运行一次，即可得到该操作的模板，开发者直接填写翻译文本即可。
 *
 * <p>用法（在项目根目录，带开发环境 classpath 运行）：
 *
 * <pre>
 * java qdream.relay.tools.GenOperationLandTemp [选项] [语言文件...]
 *
 * 选项:
 *   --all            输出所有操作的模板（默认只输出翻译不完整的操作）
 *   --output 文件    将模板写入文件（而不是输出到控制台）
 *   --help           显示帮助
 *
 * 语言文件:
 *   要检查的语言文件路径，默认:
 *     src/main/resources/assets/relay/lang/zh_cn.json
 *     src/main/resources/assets/relay/lang/en_us.json
 * </pre>
 *
 * <p>检查报告输出到 stderr，模板块输出到 stdout（便于重定向/粘贴）。
 */
public class GenOperationLandTemp {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DEFAULT_LANG_DIR = "src/main/resources/assets/relay/lang";
    private static final String[] DEFAULT_LANG_FILES = {"zh_cn.json", "en_us.json"};

    public static void main(String[] args) throws IOException {
        List<String> langFiles = new ArrayList<>();
        boolean all = false;
        Path outputFile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--all" -> all = true;
                case "--help", "-h" -> {
                    printUsage();
                    return;
                }
                case "--output" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("[错误] --output 需要文件路径");
                        printUsage();
                        return;
                    }
                    outputFile = Path.of(args[++i]);
                }
                default -> {
                    if (arg.startsWith("--output=")) {
                        outputFile = Path.of(arg.substring("--output=".length()));
                    } else if (arg.startsWith("--lang=")) {
                        langFiles.add(arg.substring("--lang=".length()));
                    } else if (!arg.isBlank()) {
                        langFiles.add(arg);
                    }
                }
            }
        }

        if (langFiles.isEmpty()) {
            for (String file : DEFAULT_LANG_FILES) {
                langFiles.add(Path.of(DEFAULT_LANG_DIR, file).toString());
            }
        }

        // 注册全部操作（仅操作注册表，不触碰 Minecraft 游戏注册表）
        RelayOperations.register();
        Map<String, List<String>> opParams = collectOperationParams();

        PrintStream templateOut = outputFile != null
                ? new PrintStream(Files.newOutputStream(outputFile), true, StandardCharsets.UTF_8)
                : System.out;

        int totalIncomplete = 0;
        for (String langFile : langFiles) {
            totalIncomplete += checkLangFile(Path.of(langFile), opParams, all, templateOut);
        }
        templateOut.flush();

        if (outputFile != null) {
            System.out.println("[完成] 模板已写入: " + outputFile);
        }
        System.out.println("[汇总] 翻译不完整的操作总数: " + totalIncomplete);
    }

    /**
     * 收集所有操作 ID -> 参数名列表（先输入后输出，按名去重），按 ID 排序保证输出稳定。
     */
    private static Map<String, List<String>> collectOperationParams() {
        Map<String, List<String>> result = new TreeMap<>();
        for (Executable executable : OperationRegistry.getAllOperation()) {
            if (executable instanceof Instruction op) {
                OperationSignature signature = op.getSignature();
                LinkedHashSet<String> params = new LinkedHashSet<>();
                for (ParameterDescriptor pd : signature.getInputs()) {
                    params.add(pd.getName());
                }
                for (ParameterDescriptor pd : signature.getOutputs()) {
                    params.add(pd.getName());
                }
                result.put(op.getId(), new ArrayList<>(params));
            }
        }
        return result;
    }

    /**
     * 检查一个语言文件的翻译完整性，并输出缺失/为空操作的模板块。
     *
     * @return 该文件中翻译不完整的操作数量
     */
    private static int checkLangFile(Path langPath, Map<String, List<String>> opParams, boolean all,
            PrintStream templateOut) throws IOException {
        if (!Files.exists(langPath)) {
            System.err.println("[错误] 语言文件不存在: " + langPath);
            return 0;
        }
        JsonObject lang = JsonParser.parseReader(Files.newBufferedReader(langPath, StandardCharsets.UTF_8))
                .getAsJsonObject();
        System.err.println("[检查] " + langPath + " (" + lang.size() + " 个键)");

        int complete = 0;
        int incomplete = 0;
        List<String> orphanOps = findOrphanOps(lang, opParams.keySet());

        for (Map.Entry<String, List<String>> entry : opParams.entrySet()) {
            String opId = entry.getKey();
            List<String> requiredKeys = buildRequiredKeys(opId, entry.getValue());
            List<String> missing = new ArrayList<>();
            List<String> empty = new ArrayList<>();

            for (String key : requiredKeys) {
                if (!lang.has(key)) {
                    missing.add(key);
                } else if (isEmptyValue(lang, key)) {
                    empty.add(key);
                }
            }

            if (missing.isEmpty() && empty.isEmpty()) {
                complete++;
                // --all 模式下也输出已完整翻译操作的模板（用于生成全新语言文件）
                if (all) {
                    printTemplateBlock(opId, entry.getValue(), templateOut);
                }
            } else {
                incomplete++;
                System.err.println("[不完整] " + opId
                        + (missing.isEmpty() ? "" : " 缺少: " + missing)
                        + (empty.isEmpty() ? "" : " 为空: " + empty));
                // 默认输出翻译不完整操作的模板，方便直接粘贴填写
                printTemplateBlock(opId, entry.getValue(), templateOut);
            }
        }

        for (String orphan : orphanOps) {
            System.err.println("[孤儿] 语言文件中存在但未注册: " + orphan);
        }

        System.err.println("[结果] 完整: " + complete + " | 不完整: " + incomplete + " | 孤儿: " + orphanOps.size());
        return incomplete;
    }

    /**
     * 构建一个操作的全部翻译键：name、description、每个参数的 description。
     */
    private static List<String> buildRequiredKeys(String opId, List<String> params) {
        List<String> keys = new ArrayList<>();
        keys.add("operation." + opId + ".name");
        keys.add("operation." + opId + ".description");
        for (String param : params) {
            keys.add("operation." + opId + ".param." + param + ".description");
        }
        return keys;
    }

    /**
     * 判断键值是否为空的字符串（""）。
     */
    private static boolean isEmptyValue(JsonObject lang, String key) {
        var element = lang.get(key);
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                && element.getAsString().isEmpty();
    }

    /**
     * 找出语言文件中存在但注册表中没有的操作 ID（孤儿键）。
     */
    private static List<String> findOrphanOps(JsonObject lang, Set<String> registeredOps) {
        List<String> orphans = new ArrayList<>();
        for (String key : lang.keySet()) {
            if (!key.startsWith("operation.")) {
                continue;
            }
            String opId = extractOpId(key);
            if (opId != null && !registeredOps.contains(opId) && !orphans.contains(opId)) {
                orphans.add(opId);
            }
        }
        return orphans;
    }

    /**
     * 从翻译键中提取操作 ID，如 "operation.relay:add.param.x.description" -> "relay:add"。
     */
    private static String extractOpId(String key) {
        String rest = key.substring("operation.".length());
        int dot = rest.indexOf('.');
        return dot < 0 ? null : rest.substring(0, dot);
    }

    /**
     * 输出一个操作的模板块（所有键值为空字符串，可直接粘贴进语言文件填写）。
     */
    private static void printTemplateBlock(String opId, List<String> params, PrintStream out) {
        List<String> keys = buildRequiredKeys(opId, params);
        for (String key : keys) {
            // 使用 Gson 序列化键与值，保证 JSON 转义正确
            out.println("  " + GSON.toJson(new JsonPrimitive(key)) + ": " + GSON.toJson(new JsonPrimitive("")) + ",");
        }
        out.println();
    }

    private static void printUsage() {
        System.out.println("""
                用法: java qdream.relay.tools.GenOperationLandTemp [选项] [语言文件...]

                自动生成操作语言模板，并检查语言文件是否包含每个操作的全部翻译键。

                选项:
                  --all            输出所有操作的模板（默认只输出翻译不完整的操作）
                  --output 文件    将模板写入文件（而不是输出到控制台）
                  --help           显示帮助

                语言文件:
                  要检查的语言文件路径，默认:
                    src/main/resources/assets/relay/lang/zh_cn.json
                    src/main/resources/assets/relay/lang/en_us.json

                示例:
                  java qdream.relay.tools.GenOperationLandTemp
                  java qdream.relay.tools.GenOperationLandTemp --lang=zh_cn.json
                  java qdream.relay.tools.GenOperationLandTemp --all --output operation_template.json
                """);
    }
}
