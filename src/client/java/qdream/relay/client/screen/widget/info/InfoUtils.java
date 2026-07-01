package qdream.relay.client.screen.widget.info;

import net.minecraft.network.chat.Component;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataFieldDescriptor;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.mc.signature.ParameterDescriptor;
import qdream.relay.mc.signature.ParameterSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 信息展示工具类
 * 提供语言文件 Key 生成、类型格式化和信息内容构建等辅助方法
 */
public class InfoUtils {

    private static final int TITLE_COLOR = 0xFF00FF00;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int TEXT_COLOR = 0xFFCCCCCC;
    private static final int TYPE_COLOR = 0xFF55FF55;
    private static final int FIELD_NAME_COLOR = 0xFF55FFFF;

    /**
     * 生成操作的语言文件 Key
     * @param operationId 操作 ID（如 "relay:pop", "relay:add"）
     * @param keyType Key 类型（如 "name", "description", "param.0"）
     * @return 完整的语言文件 Key
     */
    public static String makeOperationKey(String operationId, String keyType) {
        return "operation." + operationId + "." + keyType;
    }

    /**
     * 生成类型的语言文件 Key
     * @param typeId 类型 ID（如 "relay:number", "relay:vector"）
     * @param keyType Key 类型（如 "name", "description"）
     * @return 完整的语言文件 Key
     */
    public static String makeTypeKey(String typeId, String keyType) {
        return "type." + typeId + "." + keyType;
    }

    /**
     * 从语言文件获取文本，如果不存在则返回默认值
     */
    public static String getLanguageText(String key, String defaultValue) {
        Component component = Component.translatable(key);
        String result = component.getString();
        return result.equals(key) ? defaultValue : result;
    }

    /**
     * 获取操作的显示名称（从语言文件）
     * @param operationId 操作 ID（如 "relay:pop", "relay:add"）
     * @return 显示名称，如果语言文件不存在则返回 ID
     */
    public static String getOperationDisplayName(String operationId) {
        String key = makeOperationKey(operationId, "name");
        return getLanguageText(key, operationId);
    }

    /**
     * 获取类型的显示名称（从语言文件）
     * @param typeId 类型 ID（如 "relay:number", "relay:vector"）
     * @return 显示名称，如果语言文件不存在则返回 ID
     */
    public static String getTypeDisplayName(String typeId) {
        String key = makeTypeKey(typeId, "name");
        return getLanguageText(key, typeId);
    }

    /**
     * 构建操作的悬停信息内容
     * 第一部分：输入和输出的签名
     * 第二部分：语言文件中的描述
     * @param operationId 操作 ID
     * @param signature 操作签名
     * @return InfoContent 内容
     */
    public static HoverInfoWidget.InfoContent buildOperationInfo(String operationId, OperationSignature signature) {
        String nameKey = makeOperationKey(operationId, "name");
        String descKey = makeOperationKey(operationId, "description");

        String name = getLanguageText(nameKey, operationId);
        String desc = getLanguageText(descKey, "");

        HoverInfoWidget.InfoContent content = HoverInfoWidget.InfoContent.of(name, desc);

        if (signature == null) {
            return content;
        }

        // 第一部分：输入签名
        List<String> inputTypes = new ArrayList<>();
        
        // 从数据栈消费的参数
        for (ParameterDescriptor descriptor : signature.getConsumesFromData()) {
            inputTypes.add(formatTypes(descriptor.getTypes()));
        }
        
        // 从程序栈消费的参数
        for (ParameterDescriptor descriptor : signature.getConsumesFromProgram()) {
            inputTypes.add(formatTypes(descriptor.getTypes()) + " [程序]");
        }

        if (!inputTypes.isEmpty()) {
            content.addLine("输入：", LABEL_COLOR);
            for (String inputType : inputTypes) {
                content.addLine("  " + inputType, TYPE_COLOR);
            }
        }

        // 第一部分：输出签名
        List<String> outputTypes = new ArrayList<>();
        
        // 向数据栈生产的参数
        for (ParameterDescriptor descriptor : signature.getProducesToData()) {
            outputTypes.add(formatTypes(descriptor.getTypes()));
        }
        
        // 向程序栈生产的参数
        for (ParameterDescriptor descriptor : signature.getProducesToProgram()) {
            outputTypes.add(formatTypes(descriptor.getTypes()) + " [程序]");
        }

        if (!outputTypes.isEmpty()) {
            content.addLine("输出：", LABEL_COLOR);
            for (String outputType : outputTypes) {
                content.addLine("  " + outputType, TYPE_COLOR);
            }
        }

        return content;
    }

    /**
     * 构建类型的悬停信息内容
     * 第一部分：输入字段（构建该类型需要的字段）
     * 第二部分：语言文件中的描述
     * @param typeId 类型 ID
     * @return InfoContent 内容
     */
    public static HoverInfoWidget.InfoContent buildTypeInfo(String typeId) {
        String nameKey = makeTypeKey(typeId, "name");
        String descKey = makeTypeKey(typeId, "description");

        String name = getLanguageText(nameKey, typeId);
        String desc = getLanguageText(descKey, "");

        HoverInfoWidget.InfoContent content = HoverInfoWidget.InfoContent.of(name, desc);

        // 从注册表获取数据类型实例以获取签名
        qdream.relay.mc.OperationRegistry.getEntry(typeId).ifPresent(entry -> {
            if (entry.isDataType()) {
                var executable = entry.create();
                if (executable instanceof Data data) {
                    DataSignature signature = data.getSignature();
                    
                    // 显示输入字段（构建该类型需要的字段）
                    List<DataFieldDescriptor> inputs = signature.getInputs();
                    if (!inputs.isEmpty()) {
                        content.addLine("输入：", LABEL_COLOR);
                        for (DataFieldDescriptor field : inputs) {
                            // 格式化字段：字段名：类型|类型
                            String fieldStr = formatDataField(field);
                            content.addLine("  " + fieldStr, FIELD_NAME_COLOR);
                        }
                    }

                    // 显示输出类型
                    List<String> outputs = signature.getOutputs();
                    if (!outputs.isEmpty()) {
                        content.addLine("输出：", LABEL_COLOR);
                        for (String output : outputs) {
                            String displayName = getTypeDisplayName(output);
                            content.addLine("  " + displayName, TYPE_COLOR);
                        }
                    }
                }
            }
        });

        return content;
    }

    /**
     * 格式化数据字段描述符
     * @param field 字段描述符
     * @return 格式化后的字符串（如 "x: Number|String"）
     */
    public static String formatDataField(DataFieldDescriptor field) {
        String fieldName = field.getName();
        List<String> types = field.getTypes();

        if (types.isEmpty()) {
            return fieldName;
        }

        // 将每个类型转换为显示名称（从语言文件）
        List<String> displayTypes = new ArrayList<>();
        for (String type : types) {
            displayTypes.add(getTypeDisplayName(type));
        }

        return fieldName + ": " + String.join(" | ", displayTypes);
    }

    /**
     * 格式化类型列表为可读字符串，使用 | 拼接
     * @param types 类型列表
     * @return 格式化后的字符串（如 "Number|String"）
     */
    public static String formatTypes(List<String> types) {
        if (types.isEmpty()) {
            return "任意类型";
        }

        // 将每个类型转换为显示名称（从语言文件）
        List<String> displayTypes = new ArrayList<>();
        for (String type : types) {
            displayTypes.add(getTypeDisplayName(type));
        }

        return String.join("|", displayTypes);
    }

    /**
     * 计算信息 Widget 所需的最小高度
     * @param lineCount 内容行数
     * @return 最小高度（像素）
     */
    public static int calculateMinHeight(int lineCount) {
        int lineHeight = 10;
        int padding = 8; // 上下边距
        int titleHeight = 12; // 标题额外高度
        return padding * 2 + titleHeight + lineCount * (lineHeight + 2);
    }

    /**
     * 计算信息 Widget 所需的最小宽度
     * @param font 字体
     * @param lines 内容行
     * @return 最小宽度（像素）
     */
    public static int calculateMinWidth(net.minecraft.client.gui.Font font, java.util.List<String> lines) {
        int maxWidth = 0;
        for (String line : lines) {
            int width = font.width(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth + 16; // 左右边距
    }
}
