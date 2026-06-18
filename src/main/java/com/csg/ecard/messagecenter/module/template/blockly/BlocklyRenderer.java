package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Blockly 正文集中式渲染器。
 * <p>
 * 节点能力通过注册表维护，草稿白名单与可预览节点集合彼此独立。
 */
@Component
public class BlocklyRenderer {

    private static final String CONTENT_INPUT = "CONTENT";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SceneParamValueValidator valueValidator;
    private final Map<String, BiFunction<JsonNode, BlockRenderContext, String>> renderers;

    public BlocklyRenderer(SceneParamValueValidator valueValidator) {
        this.valueValidator = valueValidator;
        Map<String, BiFunction<JsonNode, BlockRenderContext, String>> registry = new LinkedHashMap<>();
        registry.put(BlocklyBlockTypes.MESSAGE_CONTENT, this::renderMessageContent);
        registry.put(BlocklyBlockTypes.TEXT, this::renderText);
        registry.put(BlocklyBlockTypes.TEXT_JOIN, this::renderTextJoin);
        registry.put(BlocklyBlockTypes.SCENE_PARAM_VALUE, this::renderSceneParam);
        registry.put(BlocklyBlockTypes.AMOUNT_FORMAT, this::renderAmount);
        registry.put(BlocklyBlockTypes.TIME_FORMAT, this::renderTime);
        this.renderers = Map.copyOf(registry);
    }

    /**
     * 校验模板结构中的所有节点均可渲染。
     *
     * @param blocklyJson 完整 Blockly 包装结构
     */
    public void validateRenderable(JsonNode blocklyJson) {
        JsonNode root = requireContentRoot(blocklyJson);
        validateNodeTree(root, true);
        JsonNode content = requireContentBlock(root);
        if (!hasPotentialContent(content, true)) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
    }

    /**
     * 使用示例参数渲染模板正文。
     *
     * @param blocklyJson 完整 Blockly 包装结构
     * @param sceneId     模板场景ID
     * @param params      当前场景参数
     * @param values      示例参数值
     * @return 正文、实际使用参数和警告
     */
    public BlocklyRenderResult render(JsonNode blocklyJson,
                                      Long sceneId,
                                      Map<Long, MsgSceneParam> params,
                                      Map<String, JsonNode> values) {
        validateRenderable(blocklyJson);
        JsonNode root = requireContentRoot(blocklyJson);
        BlockRenderContext context = new BlockRenderContext(sceneId, params, values, valueValidator);
        String content = renderNode(root, context, false);
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
        return new BlocklyRenderResult(content, context.getUsedParams(), context.getWarnings());
    }

    private String renderNode(JsonNode block, BlockRenderContext context, boolean includeNext) {
        String blockType = block.path("type").asText();
        BiFunction<JsonNode, BlockRenderContext, String> renderer = renderers.get(blockType);
        if (renderer == null) {
            throw unsupported(blockType);
        }
        String rendered = renderer.apply(block, context);
        if (!includeNext) {
            return rendered;
        }
        JsonNode nextBlock = activeBlock(block.path("next"));
        return nextBlock == null ? rendered : rendered + renderNode(nextBlock, context, true);
    }

    private String renderMessageContent(JsonNode block, BlockRenderContext context) {
        return renderNode(requireContentBlock(block), context, true);
    }

    private String renderText(JsonNode block, BlockRenderContext context) {
        JsonNode text = block.path("fields").get("TEXT");
        return text != null && text.isTextual() ? text.textValue() : "";
    }

    private String renderTextJoin(JsonNode block, BlockRenderContext context) {
        StringBuilder content = new StringBuilder();
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
            while (fields.hasNext()) {
                JsonNode child = activeBlock(fields.next().getValue());
                if (child != null) {
                    content.append(renderNode(child, context, false));
                }
            }
        }
        return content.toString();
    }

    private String renderSceneParam(JsonNode block, BlockRenderContext context) {
        return context.resolveParam(block);
    }

    private String renderAmount(JsonNode block, BlockRenderContext context) {
        JsonNode child = firstInputBlock(block);
        if (child == null) {
            return "";
        }
        String value = renderNode(child, context, false);
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int scale = decimalPlaces(block);
        try {
            return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED, "金额格式化节点输入必须为数字");
        }
    }

    private String renderTime(JsonNode block, BlockRenderContext context) {
        JsonNode child = firstInputBlock(block);
        if (child == null) {
            return "";
        }
        String value = renderNode(child, context, false);
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            return LocalDateTime.parse(value, TIME_FORMATTER).format(TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "时间格式化节点输入必须符合yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateNodeTree(JsonNode block, boolean root) {
        String blockType = block.path("type").asText();
        if (!renderers.containsKey(blockType)) {
            throw unsupported(blockType);
        }
        if (!root && BlocklyBlockTypes.MESSAGE_CONTENT.equals(blockType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板必须且只能存在一个message_content根节点");
        }
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
            while (fields.hasNext()) {
                JsonNode input = fields.next().getValue();
                validateWrappedBlock(activeBlock(input));
            }
        }
        JsonNode next = block.get("next");
        if (next != null && next.isObject()) {
            validateWrappedBlock(activeBlock(next));
        }
    }

    private void validateWrappedBlock(JsonNode block) {
        if (block != null && !block.isNull()) {
            validateNodeTree(block, false);
        }
    }

    private boolean hasPotentialContent(JsonNode block, boolean includeNext) {
        String blockType = block.path("type").asText();
        boolean current = BlocklyBlockTypes.TEXT.equals(blockType)
                ? hasTextField(block)
                : BlocklyBlockTypes.TEXT_JOIN.equals(blockType)
                ? hasPotentialInputContent(block)
                : BlocklyBlockTypes.SCENE_PARAM_VALUE.equals(blockType)
                || ((BlocklyBlockTypes.AMOUNT_FORMAT.equals(blockType)
                || BlocklyBlockTypes.TIME_FORMAT.equals(blockType))
                && hasPotentialFormatInput(block));
        if (current || !includeNext) {
            return current;
        }
        JsonNode next = activeBlock(block.path("next"));
        return next != null && hasPotentialContent(next, true);
    }

    private boolean hasTextField(JsonNode block) {
        JsonNode text = block.path("fields").get("TEXT");
        return text != null && text.isTextual() && StringUtils.hasText(text.textValue());
    }

    private boolean hasPotentialInputContent(JsonNode block) {
        JsonNode inputs = block.get("inputs");
        if (inputs == null || !inputs.isObject()) {
            return false;
        }
        Iterator<JsonNode> values = inputs.elements();
        while (values.hasNext()) {
            JsonNode child = activeBlock(values.next());
            if (child != null && hasPotentialContent(child, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPotentialFormatInput(JsonNode block) {
        JsonNode child = firstInputBlock(block);
        return child != null && hasPotentialContent(child, false);
    }

    private JsonNode requireContentRoot(JsonNode blocklyJson) {
        JsonNode topBlocks = blocklyJson.path("workspace").path("blocks").path("blocks");
        if (!topBlocks.isArray() || topBlocks.size() != 1
                || !BlocklyBlockTypes.MESSAGE_CONTENT.equals(topBlocks.get(0).path("type").asText())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板必须且只能存在一个message_content根节点");
        }
        return topBlocks.get(0);
    }

    private JsonNode requireContentBlock(JsonNode root) {
        JsonNode content = activeBlock(root.path("inputs").path(CONTENT_INPUT));
        if (content == null) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
        return content;
    }

    private JsonNode firstInputBlock(JsonNode block) {
        JsonNode inputs = block.get("inputs");
        if (inputs == null || !inputs.isObject()) {
            return null;
        }
        Iterator<JsonNode> values = inputs.elements();
        while (values.hasNext()) {
            JsonNode child = activeBlock(values.next());
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private JsonNode activeBlock(JsonNode wrapper) {
        if (wrapper == null || !wrapper.isObject()) {
            return null;
        }
        JsonNode block = wrapper.get("block");
        if (block != null && block.isObject()) {
            return block;
        }
        JsonNode shadow = wrapper.get("shadow");
        return shadow != null && shadow.isObject() ? shadow : null;
    }

    private int decimalPlaces(JsonNode block) {
        JsonNode fields = block.path("fields");
        for (String fieldName : new String[]{"DECIMAL_PLACES", "DECIMALS", "PRECISION"}) {
            JsonNode value = fields.get(fieldName);
            if (value != null && value.canConvertToInt() && value.intValue() >= 0) {
                return value.intValue();
            }
            if (value != null && value.isTextual()) {
                try {
                    int scale = Integer.parseInt(value.textValue());
                    if (scale >= 0) {
                        return scale;
                    }
                } catch (NumberFormatException ignored) {
                    // 非法配置统一回退到默认两位小数。
                }
            }
        }
        return 2;
    }

    private BizException unsupported(String blockType) {
        return new BizException(ErrorCode.RENDER_FAILED,
                "当前模板包含暂不支持预览的积木：" + blockType);
    }
}
