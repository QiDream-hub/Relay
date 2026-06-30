package qdream.relay.client.screen.widget.info;

import net.minecraft.network.chat.Component;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.mc.signature.ParameterDescriptor;
import qdream.relay.mc.signature.ParameterSource;

import java.util.List;

/**
 * 信息展示工具类
 * 提供语言文件 Key 生成、类型格式化和信息内容构建等辅助方法
 */
public class InfoUtils {

    private static final int TITLE_COLOR = 0xFF00FF00;
    private static final int TEXT_COLOR = 0xFFCCCCCC;

    /**
     * 生成操作的语言文件 Key
     * @param operationId 操作 ID（如 "pop", "add"）
     * @param keyType Key 类型（如 "name", "description", "param.0"）
     * @return 完整的语言文件 Key
     */
    public static String makeOperationKey(String operationId, String keyType) {
        return "operation.relay:" + operationId + "." + keyType;
    }

    /**
     * 生成类型的语言文件 Key
     * @param typeId 类型 ID（如 "number", "vector"）
     * @param keyType Key 类型（如 "name", "description"）
     * @return 完整的语言文件 Key
     */
    public static String makeTypeKey(String typeId, String keyType) {
        return "type.relay:" + typeId + "." + keyType;
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
     * 构建操作的悬停信息内容
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

        // 输入参数（从数据栈消费）
        List<ParameterDescriptor> consumesFromData = signature.getConsumesFromData();
        if (!consumesFromData.isEmpty()) {
            content.addLine("输入:", TITLE_COLOR);
            for (int i = 0; i < consumesFromData.size(); i++) {
                String paramKey = makeOperationKey(operationId, "param." + i);
                String paramDesc = getLanguageText(paramKey, formatParameter(consumesFromData.get(i)));
                content.addLine("  • " + paramDesc);
            }
        }

        // 输入参数（从程序栈消费）
        List<ParameterDescriptor> consumesFromProgram = signature.getConsumesFromProgram();
        if (!consumesFromProgram.isEmpty()) {
            if (consumesFromData.isEmpty()) {
                content.addLine("输入:", TITLE_COLOR);
            }
            for (int i = 0; i < consumesFromProgram.size(); i++) {
                String paramKey = makeOperationKey(operationId, "param." + (consumesFromData.size() + i));
                String paramDesc = getLanguageText(paramKey, formatParameter(consumesFromProgram.get(i)) + " (程序)");
                content.addLine("  • " + paramDesc);
            }
        }

        // 输出参数（向数据栈生产）
        List<ParameterDescriptor> producesToData = signature.getProducesToData();
        if (!producesToData.isEmpty()) {
            content.addLine("输出:", TITLE_COLOR);
            for (int i = 0; i < producesToData.size(); i++) {
                String outputKey = makeOperationKey(operationId, "output." + i);
                String outputDesc = getLanguageText(outputKey, formatTypes(producesToData.get(i).getTypes()));
                content.addLine("  • " + outputDesc);
            }
        }

        // 输出参数（向程序栈生产）
        List<ParameterDescriptor> producesToProgram = signature.getProducesToProgram();
        if (!producesToProgram.isEmpty()) {
            if (producesToData.isEmpty()) {
                content.addLine("输出:", TITLE_COLOR);
            }
            for (int i = 0; i < producesToProgram.size(); i++) {
                String outputKey = makeOperationKey(operationId, "output." + (producesToData.size() + i));
                String outputDesc = getLanguageText(outputKey, formatTypes(producesToProgram.get(i).getTypes()) + " (程序)");
                content.addLine("  • " + outputDesc);
            }
        }

        return content;
    }

    /**
     * 构建类型的悬停信息内容
     * @param typeId 类型 ID
     * @return InfoContent 内容
     */
    public static HoverInfoWidget.InfoContent buildTypeInfo(String typeId) {
        String nameKey = makeTypeKey(typeId, "name");
        String descKey = makeTypeKey(typeId, "description");

        String name = getLanguageText(nameKey, typeId);
        String desc = getLanguageText(descKey, "");

        HoverInfoWidget.InfoContent content = HoverInfoWidget.InfoContent.of(name, desc);

        // 类型特性
        content.addLine("特性:", TITLE_COLOR);
        switch (typeId) {
            case "number" -> {
                content.addLine("  • 支持整数和小数");
                content.addLine("  • 可参与算术运算");
            }
            case "boolean" -> {
                content.addLine("  • true 或 false");
                content.addLine("  • 可参与逻辑运算");
            }
            case "vector" -> {
                content.addLine("  • 三维向量 (x, y, z)");
                content.addLine("  • 可参与向量运算");
            }
            case "string" -> {
                content.addLine("  • 文本字符串");
                content.addLine("  • 可用于消息显示");
            }
            case "entity" -> {
                content.addLine("  • 实体引用");
                content.addLine("  • 可获取实体属性");
            }
            case "list" -> {
                content.addLine("  • 元素列表");
                content.addLine("  • 可存储任意类型");
            }
            case "null" -> {
                content.addLine("  • 空值");
                content.addLine("  • 表示无数据");
            }
            case "program" -> {
                content.addLine("  • 程序块");
                content.addLine("  • 包含可执行指令");
            }
            default -> content.addLine("  • 数据类型");
        }

        return content;
    }

    /**
     * 格式化参数描述为可读字符串
     * @param descriptor 参数描述
     * @return 格式化后的字符串
     */
    public static String formatParameter(ParameterDescriptor descriptor) {
        StringBuilder sb = new StringBuilder();

        // 类型
        if (descriptor.getTypes().isEmpty()) {
            sb.append("任意类型");
        } else {
            sb.append(String.join(" 或 ", descriptor.getTypes()));
        }

        // 来源
        ParameterSource source = descriptor.getSource();
        if (source == ParameterSource.PROGRAM_STACK) {
            sb.append(" (程序)");
        }

        return sb.toString();
    }

    /**
     * 格式化类型列表为可读字符串
     */
    private static String formatTypes(List<String> types) {
        if (types.isEmpty()) {
            return "任意类型";
        }
        return String.join(" 或 ", types);
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
