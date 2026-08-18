package qdream.relay.client.screen.widget.editor.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.DataFieldDescriptor;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.mc.signature.ParameterDescriptor;
import qdream.relay.mc.signature.ParameterSource;
import qdream.relay.tools.TextTools;

import net.minecraft.network.chat.Component;

/**
 * 信息展示工具类
 * 提供语言文件 Key 生成、类型格式化和信息内容构建等辅助方法
 */
public class InfoUtils {

    private static final int TEXT_COLOR = 0xFFCCCCCC;
    private static final int TYPE_COLOR = 0xFF55FF55;
    private static final int FIELD_NAME_COLOR = 0xFF55FFFF;

    // 签名相关颜色
    private static final int INPUT_LABEL_COLOR = 0xFF66FF66; // 输入标签颜色
    private static final int OUTPUT_LABEL_COLOR = 0xFFFF6666; // 输出标签颜色
    private static final int PARAM_TEXT_COLOR = 0xFFCCCCCC; // 参数文本颜色

    // 语言文件键
    private static final String INPUT_LABEL_KEY = "gui.relay:spell_editor.info.input_label";
    private static final String OUTPUT_LABEL_KEY = "gui.relay:spell_editor.info.output_label";
    private static final String TYPE_LABEL_KEY = "gui.relay:spell_editor.info.type_label";
    private static final String FIELD_LABEL_KEY = "gui.relay:spell_editor.info.field_label";
    private static final String PROGRAM_PREFIX_KEY = "gui.relay:spell_editor.info.program_prefix";

    public static InfoContent buildInfoContent(Executable e) {
        Double energyCost = null;
        Integer cost = null;
        List<InfoContent.InfoLine> signatureLines = new ArrayList<>();

        if (e instanceof Instruction ins) {
            energyCost = ins.getEnergy();
            cost = ins.getCost();
            OperationSignature signature = ins.getSignature();
            // 构建签名展示信息
            signatureLines = buildSignatureInfo(ins.getId(), signature);
        } else if (e instanceof Data data) {
            DataSignature signature = data.getSignature();
            signatureLines = buildDataSignatureInfo(signature);
        } else {
            return null;
        }

        return new InfoContent(
                TextTools.getId(e),
                new InfoContent.InfoLine(TextTools.getName(e).getString(), INPUT_LABEL_COLOR),
                new InfoContent.InfoLine(TextTools.getDescriptionText(e).getString(), TEXT_COLOR),
                cost,
                energyCost,
                signatureLines.isEmpty() ? null : signatureLines);
    }

    public static InfoContent buildInfoContent(String id) {
        Optional<Executable> optional = OperationRegistry.get(id);
        if (optional.isEmpty()) {
            return null;
        }
        return buildInfoContent(optional.get());
    }

    public static List<InfoContent> buildInfoContent(Set<String> ids) {
        List<InfoContent> infos = new ArrayList<>();

        ids.forEach(id -> {
            InfoContent infoContent = buildInfoContent(id);
            if (infoContent != null) {
                infos.add(infoContent);
            }
        });

        return infos;
    }

    /**
     * 构建操作签名的展示信息
     *
     * @param opId      操作 ID（如 "relay:add"），用于参数描述翻译键
     * @param signature 操作签名
     * @return 签名信息行列表
     */
    private static List<InfoContent.InfoLine> buildSignatureInfo(String opId, OperationSignature signature) {
        List<InfoContent.InfoLine> lines = new ArrayList<>();

        // 添加输入参数信息
        List<ParameterDescriptor> inputs = signature.getInputs();
        if (!inputs.isEmpty()) {
            lines.add(new InfoContent.InfoLine(Component.translatable(INPUT_LABEL_KEY).getString(), INPUT_LABEL_COLOR));
            for (ParameterDescriptor param : inputs) {
                String displayText = buildParameterDisplayText(opId, param);
                lines.add(new InfoContent.InfoLine("  " + displayText, PARAM_TEXT_COLOR));
            }
        }

        // 添加输出参数信息
        List<ParameterDescriptor> outputs = signature.getOutputs();
        if (!outputs.isEmpty()) {
            if (!inputs.isEmpty()) {
                lines.add(new InfoContent.InfoLine("", null)); // 添加空行分隔
            }
            lines.add(
                    new InfoContent.InfoLine(Component.translatable(OUTPUT_LABEL_KEY).getString(), OUTPUT_LABEL_COLOR));
            for (ParameterDescriptor param : outputs) {
                String displayText = buildParameterDisplayText(opId, param);
                lines.add(new InfoContent.InfoLine("  " + displayText, PARAM_TEXT_COLOR));
            }
        }

        return lines;
    }

    /**
     * 构建参数显示文本
     *
     * @param opId  操作 ID（如 "relay:add"），用于参数描述翻译键
     * @param param 参数描述
     * @return 格式化的显示文本
     */
    private static String buildParameterDisplayText(String opId, ParameterDescriptor param) {
        // 参数名前缀
        String prefix = "";
        if (param.getSource() == ParameterSource.PROGRAM_STACK) {
            prefix = Component.translatable(PROGRAM_PREFIX_KEY).getString();
        }

        // 类型列表
        List<String> types = param.getTypes();
        if (types == null || types.isEmpty()) {
            return null;
        }

        // 使用 Stream API 构建类型字符串
        String typesStr = types.stream()
                .map(s -> TextTools.getTypeName(s).getString())
                .collect(Collectors.joining("|"));
        return prefix + typesStr + ": " + TextTools.getParamNameText(opId, param.getName()).getString();
    }

    /**
     * 构建数据签名的展示信息
     * 直接展示 DataSignature 的 name 与 type 字段，不查询语言文件翻译
     *
     * @param signature 数据签名
     * @return 签名信息行列表
     */
    private static List<InfoContent.InfoLine> buildDataSignatureInfo(DataSignature signature) {
        List<InfoContent.InfoLine> lines = new ArrayList<>();

        // 添加输出类型信息
        List<String> outputs = signature.getOutputs();
        if (!outputs.isEmpty()) {
            lines.add(new InfoContent.InfoLine(Component.translatable(TYPE_LABEL_KEY).getString(), TYPE_COLOR));
            for (String output : outputs) {
                lines.add(new InfoContent.InfoLine("  " + TextTools.getName(output).getString(), TYPE_COLOR));
            }
        }

        // 添加输入字段信息
        List<DataFieldDescriptor> inputs = signature.getInputs();
        if (!inputs.isEmpty()) {
            if (!outputs.isEmpty()) {
                lines.add(new InfoContent.InfoLine("", null)); // 添加空行分隔
            }
            lines.add(new InfoContent.InfoLine(Component.translatable(FIELD_LABEL_KEY).getString(), FIELD_NAME_COLOR));
            for (DataFieldDescriptor field : inputs) {
                String displayText = buildFieldDisplayText(field);
                lines.add(new InfoContent.InfoLine("  " + displayText, PARAM_TEXT_COLOR));
            }
        }

        return lines;
    }

    /**
     * 构建字段显示文本
     * 直接展示字段的 name 和 type，不进行翻译
     *
     * @param field 字段描述符
     * @return 格式化的显示文本
     */
    private static String buildFieldDisplayText(DataFieldDescriptor field) {
        String name = field.getName();
        List<String> types = field.getTypes();

        if (types == null || types.isEmpty()) {
            return name;
        }

        // 使用 Stream API 构建类型字符串
        String typesStr = types.stream()
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        return name + ": " + typesStr;
    }
}