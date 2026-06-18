package com.csg.ecard.messagecenter.module.template.blockly;

import java.util.List;

/**
 * Blockly 正文渲染结果。
 *
 * @param renderedContent 渲染后的正文
 * @param usedParams      实际参与渲染的参数名
 * @param warnings        预览警告
 */
public record BlocklyRenderResult(String renderedContent,
                                  List<String> usedParams,
                                  List<String> warnings) {
}
