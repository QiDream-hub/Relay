package qdream.relay.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 错误类型 / 翻译键一致性检查工具
 *
 * <p>{@code qdream.relay.mc.errors} 包的约定是「一个错误消息一个类，一个类一个翻译键」，
 * 本工具按下面的规则校验这套约定是否被破坏：
 *
 * <ol>
 * <li>每个叶子错误类（非抽象、且没有被其它错误类继承）必须恰好使用一个 {@code error.*} 翻译键；</li>
 * <li>抽象基类不应该直接携带翻译键（消息应由具体子类提供）；</li>
 * <li>同一个翻译键不能被两个错误类共用；</li>
 * <li>用到的每个键在每份语言文件里都必须存在；</li>
 * <li>语言文件里的 {@code error.*} 键必须能在源码中找到引用（避免遗留死键）；</li>
 * <li>两份语言文件的 {@code error.*} 键集合必须一致；</li>
 * <li>同一个键在两份语言文件里的格式占位符（{@code %s}/{@code %d}/{@code %.2f}）必须一致。</li>
 * </ol>
 *
 * <p>用法（在项目根目录运行）：
 *
 * <pre>
 * ./gradlew checkErrorLangs
 * java qdream.relay.tools.CheckErrorLangs [语言文件...]
 * </pre>
 *
 * <p>检查通过返回退出码 0，发现问题返回退出码 1，报告输出到 stderr。
 */
public final class CheckErrorLangs {

    /** 错误类所在的源目录 */
    private static final Path ERROR_SRC_DIR = Path.of("src/main/java/qdream/relay/mc/errors");

    /** 扫描 {@code error.*} 字面量的源码根目录（编辑器界面在 client 源集里） */
    private static final List<Path> SOURCE_ROOTS = List.of(
            Path.of("src/main/java"),
            Path.of("src/client/java"));

    private static final String DEFAULT_LANG_DIR = "src/main/resources/assets/relay/lang";
    private static final String[] DEFAULT_LANG_FILES = {"zh_cn.json", "en_us.json"};

    private static final Pattern CLASS_DECL = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final Pattern ABSTRACT_DECL = Pattern.compile("\\babstract\\s+class\\s+(\\w+)");
    private static final Pattern EXTENDS_DECL = Pattern.compile("\\bclass\\s+\\w+\\s+extends\\s+(\\w+)");
    private static final Pattern KEY_LITERAL = Pattern.compile("\"(error\\.[A-Za-z0-9_.]+)\"");
    private static final Pattern FORMAT_SPEC = Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z%]");

    private CheckErrorLangs() {
    }

    /** 一个错误类文件的解析结果 */
    private record ErrorClass(String name, Path file, boolean isAbstract, String superClass, Set<String> keys) {
    }

    private static final List<String> problems = new ArrayList<>();
    private static final List<String> warnings = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        List<Path> langFiles = new ArrayList<>();
        for (String arg : args) {
            if (!arg.isBlank()) {
                langFiles.add(Path.of(arg));
            }
        }
        if (langFiles.isEmpty()) {
            for (String file : DEFAULT_LANG_FILES) {
                langFiles.add(Path.of(DEFAULT_LANG_DIR, file));
            }
        }

        Map<String, ErrorClass> classes = scanErrorClasses();
        Map<Path, JsonObject> langs = readLangFiles(langFiles);

        checkClassKeys(classes);
        checkKeyUniqueness(classes);
        checkKeysExistInLangs(classes, langs);
        checkDeadKeys(langs);
        checkLangKeySetsMatch(langs);
        checkFormatSpecifiersMatch(langs);

        report();
    }

    // ==================== 扫描 ====================

    /** 递归扫描 errors 包，解析出每个错误类及其使用的翻译键 */
    private static Map<String, ErrorClass> scanErrorClasses() throws IOException {
        Map<String, ErrorClass> result = new TreeMap<>();
        if (!Files.isDirectory(ERROR_SRC_DIR)) {
            problems.add("错误类源目录不存在: " + ERROR_SRC_DIR);
            return result;
        }

        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(ERROR_SRC_DIR)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("package-info.java"))
                    .sorted()
                    .forEach(files::add);
        }

        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Matcher classMatcher = CLASS_DECL.matcher(text);
            if (!classMatcher.find()) {
                problems.add("未能解析出类名: " + file);
                continue;
            }
            String name = classMatcher.group(1);
            if (result.containsKey(name)) {
                problems.add("错误类重名: " + name + "（" + file + "）");
                continue;
            }
            Matcher extendsMatcher = EXTENDS_DECL.matcher(text);
            Set<String> keys = new LinkedHashSet<>();
            Matcher keyMatcher = KEY_LITERAL.matcher(text);
            while (keyMatcher.find()) {
                keys.add(keyMatcher.group(1));
            }
            result.put(name, new ErrorClass(name, file,
                    ABSTRACT_DECL.matcher(text).find(),
                    extendsMatcher.find() ? extendsMatcher.group(1) : null,
                    keys));
        }
        return result;
    }

    /** 扫描全部源码中出现的 error.* 字面量 */
    private static Set<String> scanUsedKeys() throws IOException {
        Set<String> used = new TreeSet<>();
        for (Path root : SOURCE_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            List<Path> files = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
            for (Path file : files) {
                Matcher matcher = KEY_LITERAL.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    used.add(matcher.group(1));
                }
            }
        }
        return used;
    }

    private static Map<Path, JsonObject> readLangFiles(List<Path> langFiles) throws IOException {
        Map<Path, JsonObject> result = new LinkedHashMap<>();
        for (Path path : langFiles) {
            if (!Files.exists(path)) {
                problems.add("语言文件不存在: " + path);
                continue;
            }
            result.put(path, JsonParser
                    .parseReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))
                    .getAsJsonObject());
        }
        return result;
    }

    // ==================== 检查 ====================

    private static void checkClassKeys(Map<String, ErrorClass> classes) {
        Set<String> baseNames = new LinkedHashSet<>();
        for (ErrorClass ec : classes.values()) {
            if (ec.isAbstract()) {
                baseNames.add(ec.name());
            }
            if (ec.superClass() != null && classes.containsKey(ec.superClass())) {
                baseNames.add(ec.superClass());
            }
        }

        for (ErrorClass ec : classes.values()) {
            int count = ec.keys().size();
            if (baseNames.contains(ec.name())) {
                if (count > 0) {
                    problems.add(String.format("基类不应直接携带翻译键: %s -> %s", ec.name(), ec.keys()));
                }
            } else if (count != 1) {
                problems.add(String.format("叶子错误类必须恰好有一个翻译键: %s 有 %d 个 %s",
                        ec.name(), count, ec.keys()));
            }
        }
    }

    private static void checkKeyUniqueness(Map<String, ErrorClass> classes) {
        Map<String, List<String>> owners = new TreeMap<>();
        for (ErrorClass ec : classes.values()) {
            for (String key : ec.keys()) {
                owners.computeIfAbsent(key, k -> new ArrayList<>()).add(ec.name());
            }
        }
        for (Map.Entry<String, List<String>> entry : owners.entrySet()) {
            if (entry.getValue().size() > 1) {
                problems.add(String.format("翻译键被多个错误类共用: %s -> %s", entry.getKey(), entry.getValue()));
            }
        }
    }

    private static void checkKeysExistInLangs(Map<String, ErrorClass> classes, Map<Path, JsonObject> langs) {
        for (ErrorClass ec : classes.values()) {
            for (String key : ec.keys()) {
                for (Map.Entry<Path, JsonObject> lang : langs.entrySet()) {
                    if (!lang.getValue().has(key)) {
                        problems.add(String.format("语言文件缺少键: %s 里的 %s（来自 %s）",
                                lang.getKey().getFileName(), key, ec.name()));
                    }
                }
            }
        }
    }

    private static void checkDeadKeys(Map<Path, JsonObject> langs) throws IOException {
        Set<String> used = scanUsedKeys();
        for (Map.Entry<Path, JsonObject> lang : langs.entrySet()) {
            for (String key : lang.getValue().keySet()) {
                if (key.startsWith("error.") && !used.contains(key)) {
                    problems.add(String.format("未被引用的死键: %s 里的 %s", lang.getKey().getFileName(), key));
                }
            }
        }
    }

    private static void checkLangKeySetsMatch(Map<Path, JsonObject> langs) {
        if (langs.size() < 2) {
            return;
        }
        Map<Path, Set<String>> keySets = new LinkedHashMap<>();
        for (Map.Entry<Path, JsonObject> lang : langs.entrySet()) {
            Set<String> keys = new TreeSet<>();
            for (String key : lang.getValue().keySet()) {
                if (key.startsWith("error.")) {
                    keys.add(key);
                }
            }
            keySets.put(lang.getKey(), keys);
        }
        List<Map.Entry<Path, Set<String>>> entries = new ArrayList<>(keySets.entrySet());
        Set<String> reference = entries.get(0).getValue();
        for (int i = 1; i < entries.size(); i++) {
            Set<String> other = entries.get(i).getValue();
            Set<String> onlyReference = new TreeSet<>(reference);
            onlyReference.removeAll(other);
            Set<String> onlyOther = new TreeSet<>(other);
            onlyOther.removeAll(reference);
            if (!onlyReference.isEmpty() || !onlyOther.isEmpty()) {
                problems.add(String.format("语言文件 error.* 键集合不一致: %s 独有 %s；%s 独有 %s",
                        entries.get(0).getKey().getFileName(), onlyReference,
                        entries.get(i).getKey().getFileName(), onlyOther));
            }
        }
    }

    private static void checkFormatSpecifiersMatch(Map<Path, JsonObject> langs) {
        if (langs.size() < 2) {
            return;
        }
        List<Map.Entry<Path, JsonObject>> entries = new ArrayList<>(langs.entrySet());
        JsonObject reference = entries.get(0).getValue();
        for (int i = 1; i < entries.size(); i++) {
            JsonObject other = entries.get(i).getValue();
            for (String key : reference.keySet()) {
                if (!key.startsWith("error.") || !other.has(key)) {
                    continue;
                }
                List<String> expected = formatSpecs(reference.get(key).getAsString());
                List<String> actual = formatSpecs(other.get(key).getAsString());
                if (!expected.equals(actual)) {
                    warnings.add(String.format("占位符不一致: %s 为 %s，%s 为 %s（%s）",
                            entries.get(0).getKey().getFileName(), expected,
                            entries.get(i).getKey().getFileName(), actual, key));
                }
            }
        }
    }

    private static List<String> formatSpecs(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = FORMAT_SPEC.matcher(text);
        while (matcher.find()) {
            String spec = matcher.group();
            if (!spec.equals("%%")) {
                result.add(spec);
            }
        }
        return result;
    }

    // ==================== 报告 ====================

    private static void report() {
        PrintStream err = System.err;
        err.println("[检查] 错误类与翻译键一致性");
        for (String warning : warnings) {
            err.println("  [警告] " + warning);
        }
        if (problems.isEmpty()) {
            err.println("  [通过] 未发现问题");
            return;
        }
        for (String problem : problems) {
            err.println("  [错误] " + problem);
        }
        err.println("  [失败] 共 " + problems.size() + " 个问题");
        System.exit(1);
    }
}
