package qdream.relay.client.screen.widget.editor.tools;

import java.util.List;

// 显示信息总计
public record InfoContent(String id, InfoLine name, InfoLine description, Integer cost, Double energyCost,
        List<InfoLine> signature) {

    /**
     * 信息行
     * 
     * @param text  文本内容
     * @param color 颜色（可选）
     */
    public record InfoLine(String text, Integer color) {
    }
}
