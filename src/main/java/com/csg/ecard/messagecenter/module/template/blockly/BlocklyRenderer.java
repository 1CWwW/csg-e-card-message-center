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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Blockly 正文递归表达式渲染器。
 */
@Component
public class BlocklyRenderer {

    private static final String CONTENT_INPUT = "CONTENT";
    private static final int MAX_ELSE_IF_COUNT = 10;
    private static final int MAX_LOOP_ITERATIONS = 100;
    private static final int MAX_RENDERED_CONTENT_BYTES = 1024 * 1024;
    private static final int DIVISION_SCALE = 16;
    private static final RoundingMode DIVISION_ROUNDING = RoundingMode.HALF_UP;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SceneParamValueValidator valueValidator;

    public BlocklyRenderer(SceneParamValueValidator valueValidator) {
        this.valueValidator = valueValidator;
    }

    /**
     * 校验模板结构中的节点均属于统一支持集合且正文可输出。
     */
    public void validateRenderable(JsonNode blocklyJson) {
        JsonNode root = requireContentRoot(blocklyJson);
        JsonNode content = requireContentBlock(root);
        validateSupportedTree(root, true);
        if (!hasPotentialContent(content, true)) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
    }

    /**
     * 使用示例参数递归计算并渲染模板正文。
     */
    public BlocklyRenderResult render(JsonNode blocklyJson,
                                      Long sceneId,
                                      Map<Long, MsgSceneParam> params,
                                      Map<String, JsonNode> values) {
        validateRenderable(blocklyJson);
        JsonNode root = requireContentRoot(blocklyJson);
        BlockRenderContext context = new BlockRenderContext(sceneId, params, values, valueValidator);
        BlocklyRenderValue rendered = renderNode(root, context, false);
        String content = rendered.asText();
        if (!StringUtils.hasText(content)
                && !containsBlockType(root, BlocklyBlockTypes.CONTROLS_IF)
                && !containsBlockType(root, BlocklyBlockTypes.CONTROLS_FOR_EACH)) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
        validateRenderedContentSize(content);
        return new BlocklyRenderResult(content, context.getUsedParams(), context.getWarnings());
    }

    private BlocklyRenderValue renderNode(JsonNode block,
                                          BlockRenderContext context,
                                          boolean includeNext) {
        String blockType = block.path("type").asText();
        BlocklyRenderValue rendered = switch (blockType) {
            case BlocklyBlockTypes.MESSAGE_CONTENT -> renderMessageContent(block, context);
            case BlocklyBlockTypes.TEXT -> renderText(block);
            case BlocklyBlockTypes.TEXT_JOIN -> renderTextJoin(block, context);
            case BlocklyBlockTypes.SCENE_PARAM_VALUE -> context.resolveParam(block);
            case BlocklyBlockTypes.AMOUNT_FORMAT -> renderAmount(block, context);
            case BlocklyBlockTypes.TIME_FORMAT -> renderTime(block, context);
            case BlocklyBlockTypes.MATH_ARITHMETIC -> renderMathArithmetic(block, context);
            case BlocklyBlockTypes.MATH_MODULO -> renderMathModulo(block, context);
            case BlocklyBlockTypes.LOGIC_COMPARE -> renderLogicCompare(block, context);
            case BlocklyBlockTypes.LOGIC_OPERATION -> renderLogicOperation(block, context);
            case BlocklyBlockTypes.LOGIC_NEGATE -> renderLogicNegate(block, context);
            case BlocklyBlockTypes.STRING_CONTAINS -> renderStringContains(block, context);
            case BlocklyBlockTypes.STRING_LIKE -> renderStringLike(block, context);
            case BlocklyBlockTypes.CONTROLS_IF -> renderControlsIf(block, context);
            case BlocklyBlockTypes.CONTROLS_FOR_EACH -> renderControlsForEach(block, context);
            case BlocklyBlockTypes.LOOP_ITEM_VALUE -> renderLoopItemValue(block, context);
            default -> throw unsupported(blockType);
        };
        if (!includeNext) {
            return rendered;
        }
        JsonNode nextBlock = activeBlock(block.get("next"));
        if (nextBlock == null) {
            return rendered;
        }
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                requireText(rendered, blockType) + requireText(renderNode(nextBlock, context, true),
                        nextBlock.path("type").asText()));
    }

    private BlocklyRenderValue renderMessageContent(JsonNode block, BlockRenderContext context) {
        BlocklyRenderValue content = renderNode(requireContentBlock(block), context, true);
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                requireText(content, BlocklyBlockTypes.MESSAGE_CONTENT));
    }

    private BlocklyRenderValue renderText(JsonNode block) {
        JsonNode text = block.path("fields").get("TEXT");
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                text != null && text.isTextual() ? text.textValue() : "");
    }

    private BlocklyRenderValue renderTextJoin(JsonNode block, BlockRenderContext context) {
        StringBuilder content = new StringBuilder();
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
            while (fields.hasNext()) {
                JsonNode child = activeBlock(fields.next().getValue());
                if (child != null) {
                    content.append(requireText(renderNode(child, context, true),
                            child.path("type").asText()));
                }
            }
        }
        return new BlocklyRenderValue(BlocklyValueType.STRING, content.toString());
    }

    private BlocklyRenderValue renderAmount(JsonNode block, BlockRenderContext context) {
        BlocklyRenderValue input = renderNode(requireFirstInput(block), context, false);
        BigDecimal number = requireNumber(input, BlocklyBlockTypes.AMOUNT_FORMAT);
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                number.setScale(decimalPlaces(block), RoundingMode.HALF_UP).toPlainString());
    }

    private BlocklyRenderValue renderTime(JsonNode block, BlockRenderContext context) {
        BlocklyRenderValue input = renderNode(requireFirstInput(block), context, false);
        String value = requireValue(input, BlocklyValueType.TIME,
                "time_format 的输入必须为时间").toString();
        try {
            return new BlocklyRenderValue(BlocklyValueType.STRING,
                    LocalDateTime.parse(value, TIME_FORMATTER).format(TIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "time_format 的输入必须符合 yyyy-MM-dd HH:mm:ss");
        }
    }

    private BlocklyRenderValue renderMathArithmetic(JsonNode block, BlockRenderContext context) {
        String operator = operator(block, Set.of("ADD", "MINUS", "MULTIPLY", "DIVIDE"));
        BigDecimal left = requireNumber(renderInput(block, "A", context), BlocklyBlockTypes.MATH_ARITHMETIC);
        BigDecimal right = requireNumber(renderInput(block, "B", context), BlocklyBlockTypes.MATH_ARITHMETIC);
        BigDecimal result = switch (operator) {
            case "ADD" -> left.add(right);
            case "MINUS" -> left.subtract(right);
            case "MULTIPLY" -> left.multiply(right);
            case "DIVIDE" -> divide(left, right, BlocklyBlockTypes.MATH_ARITHMETIC);
            default -> throw unsupportedOperator(BlocklyBlockTypes.MATH_ARITHMETIC);
        };
        return new BlocklyRenderValue(BlocklyValueType.NUMBER, result);
    }

    private BlocklyRenderValue renderMathModulo(JsonNode block, BlockRenderContext context) {
        BigDecimal dividend = requireNumber(
                renderInput(block, "DIVIDEND", context), BlocklyBlockTypes.MATH_MODULO);
        BigDecimal divisor = requireNumber(
                renderInput(block, "DIVISOR", context), BlocklyBlockTypes.MATH_MODULO);
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException(ErrorCode.RENDER_FAILED, "math_modulo 的除数不能为 0");
        }
        return new BlocklyRenderValue(BlocklyValueType.NUMBER, dividend.remainder(divisor));
    }

    private BlocklyRenderValue renderLogicCompare(JsonNode block, BlockRenderContext context) {
        String operator = operator(block, Set.of("EQ", "NEQ", "LT", "LTE", "GT", "GTE"));
        BlocklyRenderValue left = renderInput(block, "A", context);
        BlocklyRenderValue right = renderInput(block, "B", context);
        boolean result;
        if ("EQ".equals(operator) || "NEQ".equals(operator)) {
            if (left.type() != right.type()) {
                throw new BizException(ErrorCode.RENDER_FAILED,
                        "logic_compare 的 EQ、NEQ 只允许同类型值比较");
            }
            boolean equal = left.type() == BlocklyValueType.NUMBER
                    ? requireNumber(left, BlocklyBlockTypes.LOGIC_COMPARE)
                    .compareTo(requireNumber(right, BlocklyBlockTypes.LOGIC_COMPARE)) == 0
                    : Objects.equals(left.value(), right.value());
            result = "EQ".equals(operator) ? equal : !equal;
        } else {
            int compared = requireNumber(left, BlocklyBlockTypes.LOGIC_COMPARE)
                    .compareTo(requireNumber(right, BlocklyBlockTypes.LOGIC_COMPARE));
            result = switch (operator) {
                case "LT" -> compared < 0;
                case "LTE" -> compared <= 0;
                case "GT" -> compared > 0;
                case "GTE" -> compared >= 0;
                default -> throw unsupportedOperator(BlocklyBlockTypes.LOGIC_COMPARE);
            };
        }
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, result);
    }

    private BlocklyRenderValue renderLogicOperation(JsonNode block, BlockRenderContext context) {
        String operator = operator(block, Set.of("AND", "OR"));
        boolean left = requireBoolean(renderInput(block, "A", context),
                BlocklyBlockTypes.LOGIC_OPERATION);
        if ("AND".equals(operator) && !left) {
            return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, false);
        }
        if ("OR".equals(operator) && left) {
            return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, true);
        }
        boolean right = requireBoolean(renderInput(block, "B", context),
                BlocklyBlockTypes.LOGIC_OPERATION);
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN,
                "AND".equals(operator) ? left && right : left || right);
    }

    private BlocklyRenderValue renderLogicNegate(JsonNode block, BlockRenderContext context) {
        boolean value = requireBoolean(renderInput(block, "BOOL", context),
                BlocklyBlockTypes.LOGIC_NEGATE);
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, !value);
    }

    private BlocklyRenderValue renderStringContains(JsonNode block, BlockRenderContext context) {
        String text = requireString(renderInput(block, "TEXT", context),
                BlocklyBlockTypes.STRING_CONTAINS, "TEXT");
        String substring = requireString(renderInput(block, "SUBSTRING", context),
                BlocklyBlockTypes.STRING_CONTAINS, "SUBSTRING");
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, text.contains(substring));
    }

    private BlocklyRenderValue renderStringLike(JsonNode block, BlockRenderContext context) {
        String text = requireString(renderInput(block, "TEXT", context),
                BlocklyBlockTypes.STRING_LIKE, "TEXT");
        String pattern = requireString(renderInput(block, "PATTERN", context),
                BlocklyBlockTypes.STRING_LIKE, "PATTERN");
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN,
                Pattern.compile(toLikeRegex(pattern), Pattern.DOTALL).matcher(text).matches());
    }

    private BlocklyRenderValue renderControlsIf(JsonNode block, BlockRenderContext context) {
        ControlsIfState state = controlsIfState(block);
        for (int index = 0; index <= state.elseIfCount(); index++) {
            boolean matched = requireBoolean(
                    renderInput(block, "IF" + index, context),
                    BlocklyBlockTypes.CONTROLS_IF);
            if (matched) {
                BlocklyRenderValue branch = renderInput(block, "DO" + index, context);
                return new BlocklyRenderValue(BlocklyValueType.STRING,
                        requireConditionalText(branch, "DO" + index));
            }
        }
        if (state.hasElse()) {
            BlocklyRenderValue branch = renderInput(block, "ELSE", context);
            return new BlocklyRenderValue(BlocklyValueType.STRING,
                    requireConditionalText(branch, "ELSE"));
        }
        return new BlocklyRenderValue(BlocklyValueType.STRING, "");
    }

    private ControlsIfState controlsIfState(JsonNode block) {
        JsonNode extraState = block.get("extraState");
        if (extraState == null || extraState.isNull()) {
            return new ControlsIfState(0, false);
        }
        int elseIfCount = extraState.path("elseIfCount").asInt(0);
        boolean hasElse = extraState.path("hasElse").asBoolean(false);
        if (elseIfCount < 0 || elseIfCount > MAX_ELSE_IF_COUNT) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_if 的 elseIfCount 不合法");
        }
        return new ControlsIfState(elseIfCount, hasElse);
    }

    private String requireConditionalText(BlocklyRenderValue value, String inputName) {
        if ((value.type() != BlocklyValueType.STRING && value.type() != BlocklyValueType.TIME)
                || value.value() == null) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_if 的 " + inputName + " 必须返回字符串");
        }
        return value.asText();
    }

    private BlocklyRenderValue renderControlsForEach(JsonNode block, BlockRenderContext context) {
        if (context.hasLoopContext()) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 暂不支持嵌套循环");
        }
        BlocklyRenderValue listValue = renderInput(block, "LIST", context);
        BlocklyValueType itemType;
        if (listValue.type() == BlocklyValueType.STRING_ARRAY) {
            itemType = BlocklyValueType.STRING;
        } else if (listValue.type() == BlocklyValueType.NUMBER_ARRAY) {
            itemType = BlocklyValueType.NUMBER;
        } else {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 的 LIST 必须返回数组");
        }
        List<?> items = listValue.value() == null ? List.of() : requireList(listValue.value());
        if (items.size() > MAX_LOOP_ITERATIONS) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 的数组元素数量不能超过 " + MAX_LOOP_ITERATIONS);
        }

        String separator = separator(block);
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            context.pushLoopContext(itemType, item);
            try {
                BlocklyRenderValue body = renderInput(block, "BODY", context);
                if (body.type() != BlocklyValueType.STRING || body.value() == null) {
                    throw new BizException(ErrorCode.RENDER_FAILED,
                            "controls_forEach 的 BODY 必须返回字符串");
                }
                if (!result.isEmpty()) {
                    result.append(separator);
                }
                result.append(body.asText());
                validateRenderedContentSize(result.toString());
            } finally {
                context.popLoopContext();
            }
        }
        return new BlocklyRenderValue(BlocklyValueType.STRING, result.toString());
    }

    private BlocklyRenderValue renderLoopItemValue(JsonNode block, BlockRenderContext context) {
        if (!context.hasLoopContext()) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "loop_item_value 只能在循环体中使用");
        }
        String itemType = block.path("extraState").path("itemType").asText();
        if (!StringUtils.hasText(itemType)) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "loop_item_value 缺少 itemType");
        }
        BlocklyValueType declaredType;
        try {
            declaredType = BlocklyValueType.valueOf(itemType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "loop_item_value 的 itemType 必须是 STRING 或 NUMBER");
        }
        if (declaredType != context.currentLoopItemType()) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "loop_item_value 的 itemType 与循环元素类型不一致");
        }
        return new BlocklyRenderValue(declaredType, context.currentLoopItemValue());
    }

    private List<?> requireList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new BizException(ErrorCode.RENDER_FAILED,
                "controls_forEach 的 LIST 必须返回数组");
    }

    private String separator(JsonNode block) {
        JsonNode separator = block.path("fields").get("SEPARATOR");
        if (separator == null) {
            return "";
        }
        if (!separator.isTextual()) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 的 SEPARATOR 必须是字符串");
        }
        return separator.textValue();
    }

    private String toLikeRegex(String likePattern) {
        StringBuilder regex = new StringBuilder("^");
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < likePattern.length(); i++) {
            char current = likePattern.charAt(i);
            if (current == '%') {
                appendQuoted(regex, literal);
                regex.append(".*");
            } else {
                literal.append(current);
            }
        }
        appendQuoted(regex, literal);
        return regex.append('$').toString();
    }

    private void appendQuoted(StringBuilder regex, StringBuilder literal) {
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }

    private BigDecimal divide(BigDecimal dividend, BigDecimal divisor, String blockType) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException(ErrorCode.RENDER_FAILED, blockType + " 的除数不能为 0");
        }
        return dividend.divide(divisor, DIVISION_SCALE, DIVISION_ROUNDING).stripTrailingZeros();
    }

    private BlocklyRenderValue renderInput(JsonNode block,
                                           String inputName,
                                           BlockRenderContext context) {
        String blockType = block.path("type").asText();
        JsonNode child = activeBlock(block.path("inputs").get(inputName));
        if (child == null) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    blockType + " 缺少输入 " + inputName);
        }
        return renderNode(child, context, false);
    }

    private BigDecimal requireNumber(BlocklyRenderValue value, String blockType) {
        return (BigDecimal) requireValue(value, BlocklyValueType.NUMBER,
                blockType + " 的输入必须为数字");
    }

    private boolean requireBoolean(BlocklyRenderValue value, String blockType) {
        return (Boolean) requireValue(value, BlocklyValueType.BOOLEAN,
                blockType + " 的输入必须为布尔值");
    }

    private String requireString(BlocklyRenderValue value, String blockType, String inputName) {
        return (String) requireValue(value, BlocklyValueType.STRING,
                blockType + " 的 " + inputName + " 必须为字符串");
    }

    private Object requireValue(BlocklyRenderValue value,
                                BlocklyValueType expected,
                                String message) {
        if (value.type() != expected || value.value() == null) {
            throw new BizException(ErrorCode.RENDER_FAILED, message);
        }
        return value.value();
    }

    private String requireText(BlocklyRenderValue value, String blockType) {
        if (!value.type().canRenderAsText()) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    blockType + " 的 BOOLEAN 结果不能直接输出为模板正文");
        }
        return value.asText();
    }

    private String operator(JsonNode block, Set<String> supported) {
        String blockType = block.path("type").asText();
        JsonNode node = block.path("fields").get("OP");
        String operator = node != null && node.isTextual() ? node.textValue() : null;
        if (!StringUtils.hasText(operator) || !supported.contains(operator)) {
            throw unsupportedOperator(blockType);
        }
        return operator;
    }

    private void validateSupportedTree(JsonNode block, boolean root) {
        String blockType = block.path("type").asText();
        if (!BlocklyBlockTypes.SUPPORTED_TYPES.contains(blockType)) {
            throw unsupported(blockType);
        }
        if (!root && BlocklyBlockTypes.MESSAGE_CONTENT.equals(blockType)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "message_content 只能作为模板根节点");
        }
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<JsonNode> iterator = inputs.elements();
            while (iterator.hasNext()) {
                JsonNode child = activeBlock(iterator.next());
                if (child != null) {
                    validateSupportedTree(child, false);
                }
            }
        }
        JsonNode next = activeBlock(block.get("next"));
        if (next != null) {
            validateSupportedTree(next, false);
        }
    }

    private boolean hasPotentialContent(JsonNode block, boolean includeNext) {
        String blockType = block.path("type").asText();
        boolean current = switch (blockType) {
            case BlocklyBlockTypes.TEXT -> hasTextField(block);
            case BlocklyBlockTypes.TEXT_JOIN -> hasPotentialInputContent(block);
            case BlocklyBlockTypes.SCENE_PARAM_VALUE,
                 BlocklyBlockTypes.AMOUNT_FORMAT,
                 BlocklyBlockTypes.TIME_FORMAT,
                 BlocklyBlockTypes.MATH_ARITHMETIC,
                 BlocklyBlockTypes.MATH_MODULO,
                 BlocklyBlockTypes.CONTROLS_IF,
                 BlocklyBlockTypes.CONTROLS_FOR_EACH -> true;
            default -> false;
        };
        if (current || !includeNext) {
            return current;
        }
        JsonNode next = activeBlock(block.get("next"));
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

    private boolean containsBlockType(JsonNode block, String expectedType) {
        if (block == null || !block.isObject()) {
            return false;
        }
        if (expectedType.equals(block.path("type").asText())) {
            return true;
        }
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<JsonNode> iterator = inputs.elements();
            while (iterator.hasNext()) {
                if (containsBlockType(activeBlock(iterator.next()), expectedType)) {
                    return true;
                }
            }
        }
        return containsBlockType(activeBlock(block.get("next")), expectedType);
    }

    private JsonNode requireContentRoot(JsonNode blocklyJson) {
        JsonNode topBlocks = blocklyJson.path("workspace").path("blocks").path("blocks");
        if (!topBlocks.isArray() || topBlocks.size() != 1
                || !BlocklyBlockTypes.MESSAGE_CONTENT.equals(topBlocks.get(0).path("type").asText())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板必须且只能存在一个 message_content 根节点");
        }
        return topBlocks.get(0);
    }

    private JsonNode requireContentBlock(JsonNode root) {
        JsonNode content = activeBlock(root.path("inputs").get(CONTENT_INPUT));
        if (content == null) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
        return content;
    }

    private JsonNode requireFirstInput(JsonNode block) {
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<JsonNode> values = inputs.elements();
            while (values.hasNext()) {
                JsonNode child = activeBlock(values.next());
                if (child != null) {
                    return child;
                }
            }
        }
        throw new BizException(ErrorCode.RENDER_FAILED,
                block.path("type").asText() + " 缺少输入");
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
                    // 非法精度配置保持兼容，回退到默认两位小数。
                }
            }
        }
        return 2;
    }

    private void validateRenderedContentSize(String content) {
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_RENDERED_CONTENT_BYTES) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "模板渲染正文不能超过 1MB");
        }
    }

    private BizException unsupported(String blockType) {
        return new BizException(ErrorCode.RENDER_FAILED,
                "当前模板包含不支持的节点类型：" + blockType);
    }

    private record ControlsIfState(int elseIfCount, boolean hasElse) {
    }

    private BizException unsupportedOperator(String blockType) {
        return new BizException(ErrorCode.RENDER_FAILED,
                blockType + " 的操作符 OP 不支持");
    }
}
