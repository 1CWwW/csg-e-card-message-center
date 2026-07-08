package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashSet;
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
    private static final String LINKED_NODE_MODE = "LINKED_NODES";

    private final SceneParamValueValidator valueValidator;

    public BlocklyRenderer(SceneParamValueValidator valueValidator) {
        this.valueValidator = valueValidator;
    }

    /**
     * 校验模板结构中的节点均属于统一支持集合且正文可输出。
     */
    public void validateRenderable(JsonNode blocklyJson) {
        if (isLinkedNodeMode(blocklyJson.path("workspace"))) {
            validateLinkedRenderable(blocklyJson);
            return;
        }
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
        if (isLinkedNodeMode(blocklyJson.path("workspace"))) {
            return renderLinkedNodes(blocklyJson, sceneId, params, values);
        }
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
            case BlocklyBlockTypes.SCENE_PARAM_VALUE, BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF ->
                    context.resolveParam(block);
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
            case BlocklyBlockTypes.LOOP_ITEM_FIELD -> renderLoopItemField(block, context);
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
        if (inputs == null || !inputs.isObject() || inputs.isEmpty()) {
            JsonNode text = block.path("fields").get("TEXT");
            return new BlocklyRenderValue(BlocklyValueType.STRING,
                    text != null && text.isTextual() ? text.textValue() : "");
        }
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
            BigDecimal leftNumber = numberOrNull(left);
            BigDecimal rightNumber = numberOrNull(right);
            if (left.type() != right.type() && (leftNumber == null || rightNumber == null)) {
                throw new BizException(ErrorCode.RENDER_FAILED,
                        "logic_compare 的 EQ、NEQ 只允许同类型值比较");
            }
            boolean equal = leftNumber != null && rightNumber != null
                    ? leftNumber.compareTo(rightNumber) == 0
                    : Objects.equals(left.value(), right.value());
            result = "EQ".equals(operator) ? equal : !equal;
        } else {
            int compared = requireGraphNumber(left, BlocklyBlockTypes.LOGIC_COMPARE)
                    .compareTo(requireGraphNumber(right, BlocklyBlockTypes.LOGIC_COMPARE));
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
        } else if (listValue.type() == BlocklyValueType.OBJECT_ARRAY) {
            itemType = BlocklyValueType.OBJECT;
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

    private BlocklyRenderValue renderLoopItemField(JsonNode block, BlockRenderContext context) {
        if (!context.hasLoopContext() || context.currentLoopItemType() != BlocklyValueType.OBJECT) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "loop_item_field 只能在对象数组循环体中使用");
        }
        JsonNode extraState = block.path("extraState");
        String fieldName = extraState.path("fieldName").asText();
        if (!StringUtils.hasText(fieldName)) {
            throw new BizException(ErrorCode.RENDER_FAILED, "loop_item_field 缺少 fieldName");
        }
        BlocklyValueType fieldType = parseLoopItemFieldType(extraState.path("fieldType").asText());
        Object item = context.currentLoopItemValue();
        if (!(item instanceof JsonNode itemNode) || !itemNode.isObject()) {
            return new BlocklyRenderValue(fieldType, null);
        }
        JsonNode fieldValue = itemNode.get(fieldName);
        if (fieldValue == null || fieldValue.isNull()) {
            return new BlocklyRenderValue(fieldType, null);
        }
        return switch (fieldType) {
            case NUMBER -> fieldValue.isNumber()
                    ? new BlocklyRenderValue(BlocklyValueType.NUMBER, fieldValue.decimalValue())
                    : new BlocklyRenderValue(BlocklyValueType.NUMBER, null);
            case STRING -> new BlocklyRenderValue(BlocklyValueType.STRING, fieldValue.asText());
            case TIME -> new BlocklyRenderValue(BlocklyValueType.TIME, fieldValue.asText());
            default -> throw new BizException(ErrorCode.RENDER_FAILED,
                    "loop_item_field 的 fieldType 必须是 STRING、NUMBER 或 TIME");
        };
    }

    private BlocklyValueType parseLoopItemFieldType(String fieldType) {
        if (!StringUtils.hasText(fieldType)) {
            throw new BizException(ErrorCode.RENDER_FAILED, "loop_item_field 缺少 fieldType");
        }
        try {
            BlocklyValueType type = BlocklyValueType.valueOf(fieldType);
            if (type == BlocklyValueType.STRING || type == BlocklyValueType.NUMBER || type == BlocklyValueType.TIME) {
                return type;
            }
        } catch (IllegalArgumentException ignored) {
            // 统一走下面的业务错误。
        }
        throw new BizException(ErrorCode.RENDER_FAILED,
                "loop_item_field 的 fieldType 必须是 STRING、NUMBER 或 TIME");
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

    private BigDecimal requireGraphNumber(BlocklyRenderValue value, String blockType) {
        if (value.value() instanceof BigDecimal number) {
            return number;
        }
        if (value.type() == BlocklyValueType.STRING && value.value() instanceof String text
                && StringUtils.hasText(text)) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                // 统一走数值类型错误。
            }
        }
        return requireNumber(value, blockType);
    }

    private BigDecimal numberOrNull(BlocklyRenderValue value) {
        if (value.value() instanceof BigDecimal number) {
            return number;
        }
        if (value.value() instanceof String text && StringUtils.hasText(text)) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
        JsonNode node = block.path("extraState").get("operation");
        if (node == null || node.isMissingNode()) {
            node = block.path("fields").get("OP");
        }
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
            case BlocklyBlockTypes.TEXT_JOIN -> hasTextField(block) || hasPotentialInputContent(block);
            case BlocklyBlockTypes.SCENE_PARAM_VALUE,
                 BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF,
                 BlocklyBlockTypes.AMOUNT_FORMAT,
                 BlocklyBlockTypes.TIME_FORMAT,
                 BlocklyBlockTypes.MATH_ARITHMETIC,
                 BlocklyBlockTypes.MATH_MODULO,
                 BlocklyBlockTypes.LOGIC_COMPARE,
                 BlocklyBlockTypes.LOGIC_OPERATION,
                 BlocklyBlockTypes.LOGIC_NEGATE,
                 BlocklyBlockTypes.STRING_CONTAINS,
                 BlocklyBlockTypes.STRING_LIKE,
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

    private void validateLinkedRenderable(JsonNode blocklyJson) {
        JsonNode workspace = blocklyJson.path("workspace");
        if (hasGraphMetadata(workspace)) {
            GraphWorkspace graph = GraphWorkspace.from(workspace, buildBlockIndex(workspace.path("blocks").path("blocks")));
            JsonNode entryBlock = graph.requireEntryBlock();
            validateSupportedTree(entryBlock, false);
            if (!hasPotentialContent(entryBlock, false)) {
                throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
            }
            return;
        }
        List<JsonNode> orderedBlocks = requireLinkedOrderedBlocks(blocklyJson);
        boolean hasContent = false;
        for (JsonNode block : orderedBlocks) {
            validateSupportedTree(block, false);
            hasContent = hasContent || hasPotentialContent(block, false);
        }
        if (!hasContent) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
    }

    private BlocklyRenderResult renderLinkedNodes(JsonNode blocklyJson,
                                                  Long sceneId,
                                                  Map<Long, MsgSceneParam> params,
                                                  Map<String, JsonNode> values) {
        validateLinkedRenderable(blocklyJson);
        BlockRenderContext context = new BlockRenderContext(sceneId, params, values, valueValidator);
        JsonNode workspace = blocklyJson.path("workspace");
        if (hasGraphMetadata(workspace)) {
            GraphWorkspace graph = GraphWorkspace.from(workspace, buildBlockIndex(workspace.path("blocks").path("blocks")));
            BlocklyRenderValue rendered = renderGraphValue(graph.entryBlockId(), graph, context, new HashSet<>());
            String content = requireText(rendered, graph.requireEntryBlock().path("type").asText());
            if (!StringUtils.hasText(content)) {
                throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
            }
            validateRenderedContentSize(content);
            return new BlocklyRenderResult(content, context.getUsedParams(), context.getWarnings());
        }
        List<JsonNode> orderedBlocks = requireLinkedOrderedBlocks(blocklyJson);
        boolean[] consumed = linkedConsumedFlags(orderedBlocks);
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < orderedBlocks.size(); index++) {
            if (consumed[index]) {
                continue;
            }
            JsonNode block = orderedBlocks.get(index);
            BlocklyRenderValue rendered = renderLinkedValue(index, orderedBlocks, context);
            content.append(rendered.asText());
            validateRenderedContentSize(content.toString());
        }
        if (!StringUtils.hasText(content.toString())) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板正文不能为空");
        }
        return new BlocklyRenderResult(content.toString(), context.getUsedParams(), context.getWarnings());
    }

    private boolean[] linkedConsumedFlags(List<JsonNode> orderedBlocks) {
        boolean[] consumed = new boolean[orderedBlocks.size()];
        for (int index = 0; index < orderedBlocks.size(); index++) {
            String blockType = orderedBlocks.get(index).path("type").asText();
            if (isLinkedBinaryOperator(blockType)) {
                if (index > 0) {
                    consumed[index - 1] = true;
                }
                if (index < orderedBlocks.size() - 1) {
                    consumed[index + 1] = true;
                }
            } else if (BlocklyBlockTypes.LOGIC_NEGATE.equals(blockType)) {
                if (index < orderedBlocks.size() - 1) {
                    consumed[index + 1] = true;
                }
            } else if (BlocklyBlockTypes.AMOUNT_FORMAT.equals(blockType)
                    || BlocklyBlockTypes.TIME_FORMAT.equals(blockType)) {
                int inputIndex = linkedAdjacentInputIndex(index, orderedBlocks);
                if (inputIndex >= 0) {
                    consumed[inputIndex] = true;
                }
            }
        }
        return consumed;
    }

    private BlocklyRenderValue renderLinkedValue(int index,
                                                 List<JsonNode> orderedBlocks,
                                                 BlockRenderContext context) {
        JsonNode block = orderedBlocks.get(index);
        String blockType = block.path("type").asText();
        return switch (blockType) {
            case BlocklyBlockTypes.TEXT -> renderText(block);
            case BlocklyBlockTypes.TEXT_JOIN -> renderTextJoin(block, context);
            case BlocklyBlockTypes.SCENE_PARAM_VALUE, BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF ->
                    context.resolveParam(block);
            case BlocklyBlockTypes.MATH_ARITHMETIC -> renderLinkedMathArithmetic(index, orderedBlocks, context);
            case BlocklyBlockTypes.MATH_MODULO -> renderLinkedModulo(index, orderedBlocks, context);
            case BlocklyBlockTypes.LOGIC_COMPARE -> renderLinkedCompare(index, orderedBlocks, context);
            case BlocklyBlockTypes.STRING_CONTAINS -> renderLinkedStringContains(index, orderedBlocks, context);
            case BlocklyBlockTypes.STRING_LIKE -> renderLinkedStringLike(index, orderedBlocks, context);
            case BlocklyBlockTypes.LOGIC_OPERATION -> renderLinkedLogicOperation(index, orderedBlocks, context);
            case BlocklyBlockTypes.LOGIC_NEGATE -> renderLinkedLogicNegate(index, orderedBlocks, context);
            case BlocklyBlockTypes.AMOUNT_FORMAT -> renderLinkedAmount(index, orderedBlocks, context);
            case BlocklyBlockTypes.TIME_FORMAT -> renderLinkedTime(index, orderedBlocks, context);
            case BlocklyBlockTypes.CONTROLS_IF ->
                    throw new BizException(ErrorCode.RENDER_FAILED, "链式条件分支暂未支持");
            case BlocklyBlockTypes.CONTROLS_FOR_EACH ->
                    throw new BizException(ErrorCode.RENDER_FAILED, "链式循环暂未支持");
            default -> renderNode(block, context, false);
        };
    }

    private BlocklyRenderValue renderGraphValue(String blockId,
                                                GraphWorkspace graph,
                                                BlockRenderContext context,
                                                Set<String> visiting) {
        JsonNode block = graph.requireBlock(blockId);
        if (!visiting.add(blockId)) {
            throw new BizException(ErrorCode.RENDER_FAILED, "Blockly 图结构存在循环引用：" + blockId);
        }
        try {
            String blockType = block.path("type").asText();
            return switch (blockType) {
                case BlocklyBlockTypes.TEXT -> prependGraphInput(blockId, graph, context, visiting, renderText(block));
                case BlocklyBlockTypes.TEXT_JOIN -> renderGraphTextJoin(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.SCENE_PARAM_VALUE, BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF ->
                        prependGraphInput(blockId, graph, context, visiting, context.resolveParam(block));
                case BlocklyBlockTypes.AMOUNT_FORMAT -> renderGraphAmount(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.TIME_FORMAT -> renderGraphTime(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.MATH_ARITHMETIC -> renderGraphMathArithmetic(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.MATH_MODULO -> renderGraphModulo(blockId, graph, context, visiting);
                case BlocklyBlockTypes.LOGIC_COMPARE -> renderGraphCompare(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.LOGIC_OPERATION -> renderGraphLogicOperation(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.LOGIC_NEGATE -> renderGraphLogicNegate(blockId, graph, context, visiting);
                case BlocklyBlockTypes.STRING_CONTAINS -> renderGraphStringContains(blockId, graph, context, visiting);
                case BlocklyBlockTypes.STRING_LIKE -> renderGraphStringLike(blockId, graph, context, visiting);
                case BlocklyBlockTypes.CONTROLS_IF -> renderGraphControlsIf(blockId, graph, context, visiting);
                case BlocklyBlockTypes.CONTROLS_FOR_EACH -> renderGraphControlsForEach(blockId, block, graph, context, visiting);
                case BlocklyBlockTypes.LOOP_ITEM_VALUE ->
                        prependGraphInput(blockId, graph, context, visiting, renderLoopItemValue(block, context));
                case BlocklyBlockTypes.LOOP_ITEM_FIELD ->
                        prependGraphInput(blockId, graph, context, visiting, renderLoopItemField(block, context));
                default -> throw unsupported(blockType);
            };
        } finally {
            visiting.remove(blockId);
        }
    }

    private BlocklyRenderValue renderGraphTextJoin(String blockId,
                                                   JsonNode block,
                                                   GraphWorkspace graph,
                                                   BlockRenderContext context,
                                                   Set<String> visiting) {
        String suffix = text(block.path("fields").get("TEXT"));
        if (suffix == null) {
            suffix = "";
        }
        String inputId = graph.inputSourceId(blockId, "input");
        if (!StringUtils.hasText(inputId)) {
            return new BlocklyRenderValue(BlocklyValueType.STRING, suffix);
        }
        BlocklyRenderValue inputValue = renderGraphValue(inputId, graph, context, visiting);
        return new BlocklyRenderValue(BlocklyValueType.STRING, inputValue.asText() + suffix);
    }

    private BlocklyRenderValue prependGraphInput(String blockId,
                                                 GraphWorkspace graph,
                                                 BlockRenderContext context,
                                                 Set<String> visiting,
                                                 BlocklyRenderValue current) {
        String inputId = graph.inputSourceId(blockId, "input");
        if (!StringUtils.hasText(inputId)) {
            return current;
        }
        JsonNode inputBlock = graph.requireBlock(inputId);
        BlocklyRenderValue prefix = renderGraphValue(inputId, graph, context, visiting);
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                requireText(prefix, inputBlock.path("type").asText()) + current.asText());
    }

    private BlocklyRenderValue renderGraphAmount(String blockId,
                                                 JsonNode block,
                                                 GraphWorkspace graph,
                                                 BlockRenderContext context,
                                                 Set<String> visiting) {
        BigDecimal number = requireGraphNumber(renderGraphInput(blockId, graph, context, visiting,
                "VALUE", "input"), BlocklyBlockTypes.AMOUNT_FORMAT);
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                number.setScale(decimalPlaces(block), RoundingMode.HALF_UP).toPlainString());
    }

    private BlocklyRenderValue renderGraphTime(String blockId,
                                               JsonNode block,
                                               GraphWorkspace graph,
                                               BlockRenderContext context,
                                               Set<String> visiting) {
        String value = requireValue(renderGraphInput(blockId, graph, context, visiting,
                "VALUE", "input"), BlocklyValueType.TIME,
                "time_format 的输入必须为时间").toString();
        try {
            return new BlocklyRenderValue(BlocklyValueType.STRING,
                    LocalDateTime.parse(value, TIME_FORMATTER).format(TIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "time_format 的输入必须符合 yyyy-MM-dd HH:mm:ss");
        }
    }

    private BlocklyRenderValue renderGraphMathArithmetic(String blockId,
                                                         JsonNode block,
                                                         GraphWorkspace graph,
                                                         BlockRenderContext context,
                                                         Set<String> visiting) {
        String operator = linkedOperator(block, Set.of("ADD", "MINUS", "MULTIPLY", "DIVIDE"));
        BigDecimal left = requireGraphNumber(renderGraphMathInput(blockId, graph, context, visiting,
                "leftValueBlockId", "A", "leftValue", "input"), BlocklyBlockTypes.MATH_ARITHMETIC);
        BigDecimal right = requireGraphNumber(renderGraphMathInput(blockId, graph, context, visiting,
                "rightValueBlockId", "B", "rightValue"), BlocklyBlockTypes.MATH_ARITHMETIC);
        BigDecimal result = switch (operator) {
            case "ADD" -> left.add(right);
            case "MINUS" -> left.subtract(right);
            case "MULTIPLY" -> left.multiply(right);
            case "DIVIDE" -> divide(left, right, BlocklyBlockTypes.MATH_ARITHMETIC);
            default -> throw unsupportedOperator(BlocklyBlockTypes.MATH_ARITHMETIC);
        };
        return new BlocklyRenderValue(BlocklyValueType.NUMBER, result);
    }

    private BlocklyRenderValue renderGraphModulo(String blockId,
                                                 GraphWorkspace graph,
                                                 BlockRenderContext context,
                                                 Set<String> visiting) {
        BigDecimal dividend = requireGraphNumber(renderGraphMathInput(blockId, graph, context, visiting,
                "leftValueBlockId", "DIVIDEND", "leftValue", "input"), BlocklyBlockTypes.MATH_MODULO);
        BigDecimal divisor = requireGraphNumber(renderGraphMathInput(blockId, graph, context, visiting,
                "rightValueBlockId", "DIVISOR", "rightValue"), BlocklyBlockTypes.MATH_MODULO);
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException(ErrorCode.RENDER_FAILED, "math_modulo 的除数不能为 0");
        }
        return new BlocklyRenderValue(BlocklyValueType.NUMBER, dividend.remainder(divisor));
    }

    private BlocklyRenderValue renderGraphCompare(String blockId,
                                                  JsonNode block,
                                                  GraphWorkspace graph,
                                                  BlockRenderContext context,
                                                  Set<String> visiting) {
        String operator = linkedOperator(block, Set.of("EQ", "NEQ", "LT", "LTE", "GT", "GTE"));
        BlocklyRenderValue left = renderGraphInput(blockId, graph, context, visiting, "leftValue", "left", "A", "input");
        BlocklyRenderValue right = renderGraphInput(blockId, graph, context, visiting, "rightValue", "right", "B");
        boolean result;
        if ("EQ".equals(operator) || "NEQ".equals(operator)) {
            BigDecimal leftNumber = numberOrNull(left);
            BigDecimal rightNumber = numberOrNull(right);
            if (left.type() != right.type() && (leftNumber == null || rightNumber == null)) {
                throw new BizException(ErrorCode.RENDER_FAILED,
                        "logic_compare 的 EQ、NEQ 只允许同类型值比较");
            }
            boolean equal = leftNumber != null && rightNumber != null
                    ? leftNumber.compareTo(rightNumber) == 0
                    : Objects.equals(left.value(), right.value());
            result = "EQ".equals(operator) ? equal : !equal;
        } else {
            int compared = requireGraphNumber(left, BlocklyBlockTypes.LOGIC_COMPARE)
                    .compareTo(requireGraphNumber(right, BlocklyBlockTypes.LOGIC_COMPARE));
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

    private BlocklyRenderValue renderGraphLogicOperation(String blockId,
                                                         JsonNode block,
                                                         GraphWorkspace graph,
                                                         BlockRenderContext context,
                                                         Set<String> visiting) {
        String operator = linkedOperator(block, Set.of("AND", "OR"));
        boolean left = requireBoolean(renderGraphInput(blockId, graph, context, visiting,
                "leftCondition", "A", "input"), BlocklyBlockTypes.LOGIC_OPERATION);
        if ("AND".equals(operator) && !left) {
            return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, false);
        }
        if ("OR".equals(operator) && left) {
            return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, true);
        }
        boolean right = requireBoolean(renderGraphInput(blockId, graph, context, visiting,
                "rightCondition", "B"), BlocklyBlockTypes.LOGIC_OPERATION);
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN,
                "AND".equals(operator) ? left && right : left || right);
    }

    private BlocklyRenderValue renderGraphLogicNegate(String blockId,
                                                      GraphWorkspace graph,
                                                      BlockRenderContext context,
                                                      Set<String> visiting) {
        boolean value = requireBoolean(renderGraphInput(blockId, graph, context, visiting,
                "BOOL", "input"), BlocklyBlockTypes.LOGIC_NEGATE);
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, !value);
    }

    private BlocklyRenderValue renderGraphStringContains(String blockId,
                                                         GraphWorkspace graph,
                                                         BlockRenderContext context,
                                                         Set<String> visiting) {
        String text = requireString(renderGraphInput(blockId, graph, context, visiting,
                "TEXT", "leftValue", "input"), BlocklyBlockTypes.STRING_CONTAINS, "TEXT");
        String substring = requireString(renderGraphInput(blockId, graph, context, visiting,
                "SUBSTRING", "rightValue"), BlocklyBlockTypes.STRING_CONTAINS, "SUBSTRING");
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, text.contains(substring));
    }

    private BlocklyRenderValue renderGraphStringLike(String blockId,
                                                     GraphWorkspace graph,
                                                     BlockRenderContext context,
                                                     Set<String> visiting) {
        String text = requireString(renderGraphInput(blockId, graph, context, visiting,
                "TEXT", "leftValue", "input"), BlocklyBlockTypes.STRING_LIKE, "TEXT");
        String pattern = requireString(renderGraphInput(blockId, graph, context, visiting,
                "PATTERN", "rightValue"), BlocklyBlockTypes.STRING_LIKE, "PATTERN");
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN,
                Pattern.compile(toLikeRegex(pattern), Pattern.DOTALL).matcher(text).matches());
    }

    private BlocklyRenderValue renderGraphControlsIf(String blockId,
                                                     GraphWorkspace graph,
                                                     BlockRenderContext context,
                                                     Set<String> visiting) {
        JsonNode branch = graph.requireBranch(blockId);
        String conditionBlockId = requiredGraphText(branch, "conditionBlockId", "controls_if 缺少 conditionBlockId");
        String thenBlockId = requiredGraphText(branch, "thenBlockId", "controls_if 缺少 thenBlockId");
        String elseBlockId = requiredGraphText(branch, "elseBlockId", "controls_if 缺少 elseBlockId");
        boolean matched = requireBoolean(renderGraphValue(conditionBlockId, graph, context, visiting),
                BlocklyBlockTypes.CONTROLS_IF);
        String targetBlockId = matched ? thenBlockId : elseBlockId;
        JsonNode targetBlock = graph.requireBlock(targetBlockId);
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                requireConditionalText(renderGraphValue(targetBlockId, graph, context, visiting),
                        targetBlock.path("type").asText()));
    }

    private BlocklyRenderValue renderGraphControlsForEach(String blockId,
                                                          JsonNode block,
                                                          GraphWorkspace graph,
                                                          BlockRenderContext context,
                                                          Set<String> visiting) {
        if (context.hasLoopContext()) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 暂不支持嵌套循环");
        }
        JsonNode loop = graph.requireLoop(blockId);
        String collectionBlockId = text(loop.get("collectionBlockId"));
        if (!StringUtils.hasText(collectionBlockId)) {
            collectionBlockId = graph.inputSourceId(blockId, "collection", "LIST");
        }
        if (!StringUtils.hasText(collectionBlockId)) {
            throw new BizException(ErrorCode.RENDER_FAILED, "controls_forEach 缺少 collectionBlockId");
        }
        String bodyBlockId = requiredGraphText(loop, "bodyBlockId", "controls_forEach 缺少 bodyBlockId");
        BlocklyRenderValue listValue = renderGraphValue(collectionBlockId, graph, context, visiting);
        BlocklyValueType itemType;
        if (listValue.type() == BlocklyValueType.STRING_ARRAY) {
            itemType = BlocklyValueType.STRING;
        } else if (listValue.type() == BlocklyValueType.NUMBER_ARRAY) {
            itemType = BlocklyValueType.NUMBER;
        } else if (listValue.type() == BlocklyValueType.OBJECT_ARRAY) {
            itemType = BlocklyValueType.OBJECT;
        } else {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 的 collectionBlockId 必须返回数组");
        }
        List<?> items = listValue.value() == null ? List.of() : requireList(listValue.value());
        if (items.size() > MAX_LOOP_ITERATIONS) {
            throw new BizException(ErrorCode.RENDER_FAILED,
                    "controls_forEach 鐨勬暟缁勫厓绱犳暟閲忎笉鑳借秴杩?" + MAX_LOOP_ITERATIONS);
        }
        String separator = separator(block);
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            context.pushLoopContext(itemType, item);
            try {
                JsonNode bodyBlock = graph.requireBlock(bodyBlockId);
                BlocklyRenderValue body = renderGraphValue(bodyBlockId, graph, context, visiting);
                if (body.type() != BlocklyValueType.STRING || body.value() == null) {
                    throw new BizException(ErrorCode.RENDER_FAILED,
                            "controls_forEach 的 bodyBlockId 必须返回字符串");
                }
                if (!result.isEmpty()) {
                    result.append(separator);
                }
                result.append(requireText(body, bodyBlock.path("type").asText()));
                validateRenderedContentSize(result.toString());
            } finally {
                context.popLoopContext();
            }
        }
        return prependGraphInput(blockId, graph, context, visiting,
                new BlocklyRenderValue(BlocklyValueType.STRING, result.toString()));
    }

    private BlocklyRenderValue renderGraphInput(String blockId,
                                                GraphWorkspace graph,
                                                BlockRenderContext context,
                                                Set<String> visiting,
                                                String... ports) {
        String sourceId = graph.inputSourceId(blockId, ports);
        if (!StringUtils.hasText(sourceId)) {
            JsonNode block = graph.requireBlock(blockId);
            for (String port : ports) {
                JsonNode child = activeBlock(block.path("inputs").get(port));
                if (child != null) {
                    return renderNode(child, context, false);
                }
            }
            throw new BizException(ErrorCode.RENDER_FAILED,
                    block.path("type").asText() + " 缺少输入 " + ports[0]);
        }
        return renderGraphValue(sourceId, graph, context, visiting);
    }

    private BlocklyRenderValue renderGraphMathInput(String blockId,
                                                    GraphWorkspace graph,
                                                    BlockRenderContext context,
                                                    Set<String> visiting,
                                                    String expressionField,
                                                    String... fallbackPorts) {
        String sourceId = graph.mathInputSourceId(blockId, expressionField);
        if (StringUtils.hasText(sourceId)) {
            return renderGraphValue(sourceId, graph, context, visiting);
        }
        return renderGraphInput(blockId, graph, context, visiting, fallbackPorts);
    }

    private BlocklyRenderValue renderLinkedMathArithmetic(int index,
                                                          List<JsonNode> orderedBlocks,
                                                          BlockRenderContext context) {
        String operator = linkedOperator(orderedBlocks.get(index), Set.of("ADD", "MINUS", "MULTIPLY", "DIVIDE"));
        BigDecimal left = requireLinkedNumber(linkedOperand(index - 1, orderedBlocks, context),
                "数学运算节点前后必须是数值");
        BigDecimal right = requireLinkedNumber(linkedOperand(index + 1, orderedBlocks, context),
                "数学运算节点前后必须是数值");
        BigDecimal result = switch (operator) {
            case "ADD" -> left.add(right);
            case "MINUS" -> left.subtract(right);
            case "MULTIPLY" -> left.multiply(right);
            case "DIVIDE" -> divide(left, right, BlocklyBlockTypes.MATH_ARITHMETIC);
            default -> throw unsupportedOperator(BlocklyBlockTypes.MATH_ARITHMETIC);
        };
        return new BlocklyRenderValue(BlocklyValueType.NUMBER, result);
    }

    private BlocklyRenderValue renderLinkedModulo(int index,
                                                  List<JsonNode> orderedBlocks,
                                                  BlockRenderContext context) {
        BigDecimal dividend = requireLinkedNumber(linkedOperand(index - 1, orderedBlocks, context),
                "取余节点前后必须是数值");
        BigDecimal divisor = requireLinkedNumber(linkedOperand(index + 1, orderedBlocks, context),
                "取余节点前后必须是数值");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException(ErrorCode.RENDER_FAILED, "math_modulo 的除数不能为 0");
        }
        return new BlocklyRenderValue(BlocklyValueType.NUMBER, dividend.remainder(divisor));
    }

    private BlocklyRenderValue renderLinkedCompare(int index,
                                                   List<JsonNode> orderedBlocks,
                                                   BlockRenderContext context) {
        String operator = linkedOperator(orderedBlocks.get(index), Set.of("EQ", "NEQ", "LT", "LTE", "GT", "GTE"));
        BlocklyRenderValue left = linkedOperand(index - 1, orderedBlocks, context);
        BlocklyRenderValue right = linkedOperand(index + 1, orderedBlocks, context);
        boolean result;
        if ("EQ".equals(operator) || "NEQ".equals(operator)) {
            BigDecimal leftNumber = numberOrNull(left);
            BigDecimal rightNumber = numberOrNull(right);
            boolean equal = leftNumber != null && rightNumber != null
                    ? leftNumber.compareTo(rightNumber) == 0
                    : Objects.equals(left.asText(), right.asText());
            result = "EQ".equals(operator) ? equal : !equal;
        } else {
            int compared = requireLinkedNumber(left, "比较节点前后必须是数值")
                    .compareTo(requireLinkedNumber(right, "比较节点前后必须是数值"));
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

    private BlocklyRenderValue renderLinkedStringContains(int index,
                                                          List<JsonNode> orderedBlocks,
                                                          BlockRenderContext context) {
        String left = linkedOperand(index - 1, orderedBlocks, context).asText();
        String right = linkedOperand(index + 1, orderedBlocks, context).asText();
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, left.contains(right));
    }

    private BlocklyRenderValue renderLinkedStringLike(int index,
                                                      List<JsonNode> orderedBlocks,
                                                      BlockRenderContext context) {
        String left = linkedOperand(index - 1, orderedBlocks, context).asText();
        String right = linkedOperand(index + 1, orderedBlocks, context).asText();
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN,
                Pattern.compile(toLikeRegex(right), Pattern.DOTALL).matcher(left).matches());
    }

    private BlocklyRenderValue renderLinkedLogicOperation(int index,
                                                          List<JsonNode> orderedBlocks,
                                                          BlockRenderContext context) {
        String operator = linkedOperator(orderedBlocks.get(index), Set.of("AND", "OR"));
        boolean left = requireLinkedBoolean(linkedOperand(index - 1, orderedBlocks, context),
                "逻辑运算节点前后必须是布尔值");
        if ("AND".equals(operator) && !left) {
            return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, false);
        }
        if ("OR".equals(operator) && left) {
            return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, true);
        }
        boolean right = requireLinkedBoolean(linkedOperand(index + 1, orderedBlocks, context),
                "逻辑运算节点前后必须是布尔值");
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN,
                "AND".equals(operator) ? left && right : left || right);
    }

    private BlocklyRenderValue renderLinkedLogicNegate(int index,
                                                       List<JsonNode> orderedBlocks,
                                                       BlockRenderContext context) {
        boolean value = requireLinkedBoolean(linkedOperand(index + 1, orderedBlocks, context),
                "logic_negate 后一个节点必须是 boolean");
        return new BlocklyRenderValue(BlocklyValueType.BOOLEAN, !value);
    }

    private BlocklyRenderValue renderLinkedAmount(int index,
                                                  List<JsonNode> orderedBlocks,
                                                  BlockRenderContext context) {
        int inputIndex = linkedAdjacentInputIndex(index, orderedBlocks);
        BigDecimal number = requireLinkedNumber(linkedOperand(inputIndex, orderedBlocks, context),
                "金额格式化节点相邻节点必须是数值");
        return new BlocklyRenderValue(BlocklyValueType.STRING,
                number.setScale(decimalPlaces(orderedBlocks.get(index)), RoundingMode.HALF_UP).toPlainString());
    }

    private BlocklyRenderValue renderLinkedTime(int index,
                                                List<JsonNode> orderedBlocks,
                                                BlockRenderContext context) {
        int inputIndex = linkedAdjacentInputIndex(index, orderedBlocks);
        String value = linkedTimeInputText(inputIndex, orderedBlocks, context);
        String format = linkedTimeFormat(orderedBlocks.get(index));
        LocalDateTime time = parseLinkedTime(value);
        DateTimeFormatter formatter = requireLinkedTimeFormatter(format);
        return new BlocklyRenderValue(BlocklyValueType.STRING, time.format(formatter));
    }

    private BlocklyRenderValue linkedOperand(int index,
                                             List<JsonNode> orderedBlocks,
                                             BlockRenderContext context) {
        if (index < 0 || index >= orderedBlocks.size()) {
            throw new BizException(ErrorCode.RENDER_FAILED, "链式节点缺少相邻操作数");
        }
        return renderLinkedValue(index, orderedBlocks, context);
    }

    private List<JsonNode> requireLinkedOrderedBlocks(JsonNode blocklyJson) {
        JsonNode workspace = blocklyJson.path("workspace");
        JsonNode order = workspace.get("templateNodeOrder");
        if (order == null || !order.isArray() || order.isEmpty()) {
            throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板内容不能为空");
        }
        Map<String, JsonNode> blockIndex = buildBlockIndex(workspace.path("blocks").path("blocks"));
        List<JsonNode> result = new java.util.ArrayList<>();
        for (JsonNode idNode : order) {
            String blockId = text(idNode);
            if (!StringUtils.hasText(blockId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "templateNodeOrder 中的节点ID不能为空");
            }
            JsonNode block = blockIndex.get(blockId);
            if (block == null) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "templateNodeOrder 引用了不存在的 block id：" + blockId);
            }
            result.add(block);
        }
        return result;
    }

    private Map<String, JsonNode> buildBlockIndex(JsonNode topBlocks) {
        if (topBlocks == null || !topBlocks.isArray()) {
            return Map.of();
        }
        Map<String, JsonNode> result = new java.util.LinkedHashMap<>();
        for (JsonNode block : topBlocks) {
            collectBlockIndex(block, result);
        }
        return result;
    }

    private void collectBlockIndex(JsonNode node, Map<String, JsonNode> result) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            if (node.has("type")) {
                String blockId = text(node.get("id"));
                if (StringUtils.hasText(blockId)) {
                    result.putIfAbsent(blockId, node);
                }
            }
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectBlockIndex(elements.next(), result);
            }
            return;
        }
        if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectBlockIndex(elements.next(), result);
            }
        }
    }

    private boolean isLinkedBinaryOperator(String blockType) {
        return BlocklyBlockTypes.MATH_ARITHMETIC.equals(blockType)
                || BlocklyBlockTypes.MATH_MODULO.equals(blockType)
                || BlocklyBlockTypes.LOGIC_COMPARE.equals(blockType)
                || BlocklyBlockTypes.STRING_CONTAINS.equals(blockType)
                || BlocklyBlockTypes.STRING_LIKE.equals(blockType)
                || BlocklyBlockTypes.LOGIC_OPERATION.equals(blockType);
    }

    private int linkedAdjacentInputIndex(int index, List<JsonNode> orderedBlocks) {
        if (index > 0) {
            return index - 1;
        }
        if (index < orderedBlocks.size() - 1) {
            return index + 1;
        }
        throw new BizException(ErrorCode.RENDER_FAILED, "格式化节点必须存在相邻输入节点");
    }

    private String linkedOperator(JsonNode block, Set<String> supported) {
        String blockType = block.path("type").asText();
        String operator = text(block.path("extraState").get("operation"));
        if (!StringUtils.hasText(operator)) {
            operator = text(block.path("fields").get("OP"));
        }
        if (!StringUtils.hasText(operator) || !supported.contains(operator)) {
            throw new BizException(ErrorCode.RENDER_FAILED, blockType + " " + operator + " 不支持");
        }
        return operator;
    }

    private BigDecimal requireLinkedNumber(BlocklyRenderValue value, String message) {
        if (value.value() instanceof BigDecimal number) {
            return number;
        }
        String text = value.asText();
        if (!StringUtils.hasText(text)) {
            throw new BizException(ErrorCode.RENDER_FAILED, message);
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED, message);
        }
    }

    private boolean requireLinkedBoolean(BlocklyRenderValue value, String message) {
        if (value.value() instanceof Boolean bool) {
            return bool;
        }
        String text = value.asText();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new BizException(ErrorCode.RENDER_FAILED, message);
    }

    private String linkedTimeFormat(JsonNode block) {
        String format = text(block.path("extraState").get("format"));
        if (StringUtils.hasText(format)) {
            return format;
        }
        format = text(block.path("fields").get("FORMAT"));
        return StringUtils.hasText(format) ? format : "yyyy-MM-dd HH:mm:ss";
    }

    private LocalDateTime parseLinkedTime(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.RENDER_FAILED, "时间格式化节点相邻节点不能为空");
        }
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种兼容格式。
            }
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // 尝试 ISO_LOCAL_DATE_TIME。
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // 统一返回业务错误。
        }
        throw new BizException(ErrorCode.RENDER_FAILED,
                "时间值格式不合法，应为 yyyy-MM-dd HH:mm:ss");
    }

    private DateTimeFormatter requireLinkedTimeFormatter(String format) {
        try {
            return DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.RENDER_FAILED, "时间格式模板不合法：" + format);
        }
    }

    private String linkedTimeInputText(int inputIndex,
                                       List<JsonNode> orderedBlocks,
                                       BlockRenderContext context) {
        JsonNode inputBlock = orderedBlocks.get(inputIndex);
        if (BlocklyBlockTypes.isSceneParamType(inputBlock.path("type").asText())) {
            return context.resolveRawParamText(inputBlock);
        }
        return linkedOperand(inputIndex, orderedBlocks, context).asText();
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
        JsonNode decimals = block.path("extraState").get("decimals");
        if (decimals != null && decimals.canConvertToInt() && decimals.intValue() >= 0) {
            return decimals.intValue();
        }
        if (decimals != null && decimals.isTextual()) {
            try {
                int scale = Integer.parseInt(decimals.textValue());
                if (scale >= 0) {
                    return scale;
                }
            } catch (NumberFormatException ignored) {
                // 非法精度配置保持兼容，继续尝试旧字段。
            }
        }
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

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private boolean isLinkedNodeMode(JsonNode workspace) {
        return workspace != null
                && workspace.isObject()
                && LINKED_NODE_MODE.equals(text(workspace.get("templateNodeMode")));
    }

    private boolean hasGraphMetadata(JsonNode workspace) {
        return workspace != null
                && workspace.isObject()
                && ((workspace.path("templateLinks").isArray() && !workspace.path("templateLinks").isEmpty())
                || (workspace.path("templateMathExpressions").isObject()
                && !workspace.path("templateMathExpressions").isEmpty())
                || (workspace.path("templateBranches").isObject() && !workspace.path("templateBranches").isEmpty())
                || (workspace.path("templateLoops").isObject() && !workspace.path("templateLoops").isEmpty()));
    }

    private String requiredGraphText(JsonNode node, String field, String message) {
        String value = text(node.get(field));
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.RENDER_FAILED, message);
        }
        return value;
    }

    private BizException unsupported(String blockType) {
        return new BizException(ErrorCode.RENDER_FAILED,
                "当前模板包含不支持的节点类型：" + blockType);
    }

    private record ControlsIfState(int elseIfCount, boolean hasElse) {
    }

    private static final class GraphWorkspace {

        private final Map<String, JsonNode> blocks;
        private final Map<String, Map<String, String>> inputs;
        private final Map<String, List<String>> orderedInputs;
        private final Map<String, JsonNode> mathExpressions;
        private final Map<String, JsonNode> branches;
        private final Map<String, JsonNode> loops;
        private final String entryBlockId;

        private GraphWorkspace(Map<String, JsonNode> blocks,
                               Map<String, Map<String, String>> inputs,
                               Map<String, List<String>> orderedInputs,
                               Map<String, JsonNode> mathExpressions,
                               Map<String, JsonNode> branches,
                               Map<String, JsonNode> loops,
                               String entryBlockId) {
            this.blocks = blocks;
            this.inputs = inputs;
            this.orderedInputs = orderedInputs;
            this.mathExpressions = mathExpressions;
            this.branches = branches;
            this.loops = loops;
            this.entryBlockId = entryBlockId;
        }

        private static GraphWorkspace from(JsonNode workspace, Map<String, JsonNode> blockMap) {
            Map<String, Map<String, String>> inputs = new LinkedHashMap<>();
            Map<String, List<String>> orderedInputs = new LinkedHashMap<>();
            JsonNode links = workspace.path("templateLinks");
            if (links.isArray()) {
                for (JsonNode link : links) {
                    String sourceId = firstText(link, "sourceId", "sourceBlockId", "fromBlockId", "fromId");
                    String targetId = firstText(link, "targetId", "targetBlockId", "toBlockId", "toId");
                    if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)
                            || sourceId.equals(targetId)) {
                        continue;
                    }
                    String sourcePort = firstText(link, "sourcePort", "sourceOutput", "fromPort");
                    if (StringUtils.hasText(sourcePort) && !"output".equals(sourcePort)) {
                        continue;
                    }
                    String targetPort = firstText(link, "targetPort", "targetInput");
                    if (!StringUtils.hasText(targetPort)) {
                        targetPort = inferredTargetPort(blockMap.get(targetId));
                    }
                    targetPort = supportedTargetPort(blockMap.get(targetId), targetPort);
                    if (!StringUtils.hasText(targetPort)) {
                        continue;
                    }
                    inputs.computeIfAbsent(targetId, ignored -> new LinkedHashMap<>())
                            .putIfAbsent(targetPort, sourceId);
                    orderedInputs.computeIfAbsent(targetId, ignored -> new ArrayList<>()).add(sourceId);
                }
            }
            Map<String, JsonNode> mathExpressions = objectFields(workspace.path("templateMathExpressions"));
            Map<String, JsonNode> branches = objectFields(workspace.path("templateBranches"));
            Map<String, JsonNode> loops = objectFields(workspace.path("templateLoops"));
            Set<String> dependencyIds = dependencyIds(inputs, mathExpressions, branches, loops);
            String entryBlockId = firstText(workspace, "templateEntryBlockId", "entryBlockId");
            String inferredRootId = inferredRootId(blockMap, branches, loops, dependencyIds);
            if (StringUtils.hasText(inferredRootId)
                    && (!StringUtils.hasText(entryBlockId)
                    || dependencyIds.contains(entryBlockId)
                    || !isGraphRootBlock(blockMap.get(entryBlockId)))) {
                entryBlockId = inferredRootId;
            }
            if (!StringUtils.hasText(entryBlockId) && !blockMap.isEmpty()) {
                entryBlockId = blockMap.keySet().iterator().next();
            }
            return new GraphWorkspace(blockMap, inputs, orderedInputs, mathExpressions, branches, loops, entryBlockId);
        }

        private String entryBlockId() {
            return entryBlockId;
        }

        private JsonNode requireEntryBlock() {
            if (!StringUtils.hasText(entryBlockId)) {
                throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板内容不能为空");
            }
            return requireBlock(entryBlockId);
        }

        private JsonNode requireBlock(String blockId) {
            JsonNode block = blocks.get(blockId);
            if (block == null) {
                throw new BizException(ErrorCode.RENDER_FAILED, "模板节点不存在：" + blockId);
            }
            return block;
        }

        private JsonNode requireBranch(String blockId) {
            JsonNode branch = branches.get(blockId);
            if (branch == null || !branch.isObject()) {
                throw new BizException(ErrorCode.RENDER_FAILED, "controls_if 缺少分支结构：" + blockId);
            }
            return branch;
        }

        private JsonNode requireLoop(String blockId) {
            JsonNode loop = loops.get(blockId);
            if (loop == null || !loop.isObject()) {
                throw new BizException(ErrorCode.RENDER_FAILED, "controls_forEach 缺少循环结构：" + blockId);
            }
            return loop;
        }

        private String inputSourceId(String targetId, String... ports) {
            Map<String, String> byPort = inputs.get(targetId);
            if (byPort == null) {
                return null;
            }
            for (String port : ports) {
                String sourceId = byPort.get(port);
                if (StringUtils.hasText(sourceId)) {
                    return sourceId;
                }
            }
            return null;
        }

        private List<String> inputSourceIds(String targetId) {
            return orderedInputs.getOrDefault(targetId, List.of());
        }

        private String mathInputSourceId(String blockId, String field) {
            JsonNode expression = mathExpressions.get(blockId);
            return firstText(expression, field);
        }

        private static Set<String> dependencyIds(Map<String, Map<String, String>> inputs,
                                                 Map<String, JsonNode> mathExpressions,
                                                 Map<String, JsonNode> branches,
                                                 Map<String, JsonNode> loops) {
            Set<String> result = new HashSet<>();
            for (Map<String, String> byPort : inputs.values()) {
                result.addAll(byPort.values());
            }
            for (JsonNode expression : mathExpressions.values()) {
                addText(result, expression, "leftValueBlockId");
                addText(result, expression, "rightValueBlockId");
            }
            for (JsonNode branch : branches.values()) {
                addText(result, branch, "conditionBlockId");
                addText(result, branch, "thenBlockId");
                addText(result, branch, "elseBlockId");
            }
            for (JsonNode loop : loops.values()) {
                addText(result, loop, "collectionBlockId");
                addText(result, loop, "bodyBlockId");
            }
            return result;
        }

        private static String inferredRootId(Map<String, JsonNode> blocks,
                                             Map<String, JsonNode> branches,
                                             Map<String, JsonNode> loops,
                                             Set<String> dependencyIds) {
            String rootId = singleRoot(branches.keySet(), dependencyIds);
            if (StringUtils.hasText(rootId)) {
                return rootId;
            }
            rootId = singleRoot(loops.keySet(), dependencyIds);
            if (StringUtils.hasText(rootId)) {
                return rootId;
            }
            rootId = singleRoot(blocks.keySet(), dependencyIds);
            if (StringUtils.hasText(rootId)) {
                return rootId;
            }
            String candidate = null;
            for (Map.Entry<String, JsonNode> entry : blocks.entrySet()) {
                String blockType = entry.getValue().path("type").asText();
                if ((BlocklyBlockTypes.CONTROLS_IF.equals(blockType)
                        || BlocklyBlockTypes.CONTROLS_FOR_EACH.equals(blockType))
                        && !dependencyIds.contains(entry.getKey())) {
                    if (candidate != null) {
                        return null;
                    }
                    candidate = entry.getKey();
                }
            }
            return candidate;
        }

        private static String singleRoot(Set<String> ids, Set<String> dependencyIds) {
            String candidate = null;
            for (String id : ids) {
                if (dependencyIds.contains(id)) {
                    continue;
                }
                if (candidate != null) {
                    return null;
                }
                candidate = id;
            }
            return candidate;
        }

        private static void addText(Set<String> values, JsonNode node, String field) {
            String value = firstText(node, field);
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }

        private static boolean isGraphRootBlock(JsonNode block) {
            if (block == null || !block.isObject()) {
                return false;
            }
            String blockType = block.path("type").asText();
            return BlocklyBlockTypes.CONTROLS_IF.equals(blockType)
                    || BlocklyBlockTypes.CONTROLS_FOR_EACH.equals(blockType);
        }

        private static String inferredTargetPort(JsonNode targetBlock) {
            if (targetBlock == null || !targetBlock.isObject()) {
                return null;
            }
            String blockType = targetBlock.path("type").asText();
            return switch (blockType) {
                case BlocklyBlockTypes.TEXT,
                        BlocklyBlockTypes.SCENE_PARAM_VALUE,
                        BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF,
                        BlocklyBlockTypes.LOOP_ITEM_VALUE,
                        BlocklyBlockTypes.LOOP_ITEM_FIELD,
                        BlocklyBlockTypes.CONTROLS_FOR_EACH,
                        BlocklyBlockTypes.TEXT_JOIN,
                        BlocklyBlockTypes.AMOUNT_FORMAT,
                        BlocklyBlockTypes.TIME_FORMAT,
                        BlocklyBlockTypes.LOGIC_NEGATE -> "input";
                default -> null;
            };
        }

        private static String supportedTargetPort(JsonNode targetBlock, String targetPort) {
            if (targetBlock == null || !targetBlock.isObject() || !StringUtils.hasText(targetPort)) {
                return null;
            }
            String blockType = targetBlock.path("type").asText();
            return switch (blockType) {
                case BlocklyBlockTypes.TEXT,
                        BlocklyBlockTypes.SCENE_PARAM_VALUE,
                        BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF,
                        BlocklyBlockTypes.LOOP_ITEM_VALUE,
                        BlocklyBlockTypes.LOOP_ITEM_FIELD ->
                        "input".equals(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.CONTROLS_FOR_EACH ->
                        Set.of("input", "collection", "LIST").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.TEXT_JOIN -> targetPort;
                case BlocklyBlockTypes.AMOUNT_FORMAT, BlocklyBlockTypes.TIME_FORMAT ->
                        Set.of("VALUE", "input").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.MATH_ARITHMETIC ->
                        Set.of("A", "B", "leftValue", "rightValue").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.MATH_MODULO ->
                        Set.of("DIVIDEND", "DIVISOR", "leftValue", "rightValue").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.LOGIC_COMPARE ->
                        Set.of("A", "B", "leftValue", "rightValue", "left", "right").contains(targetPort)
                                ? targetPort : null;
                case BlocklyBlockTypes.LOGIC_OPERATION ->
                        Set.of("A", "B", "leftCondition", "rightCondition").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.LOGIC_NEGATE ->
                        Set.of("BOOL", "input").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.STRING_CONTAINS ->
                        Set.of("TEXT", "SUBSTRING", "leftValue", "rightValue").contains(targetPort) ? targetPort : null;
                case BlocklyBlockTypes.STRING_LIKE ->
                        Set.of("TEXT", "PATTERN", "leftValue", "rightValue").contains(targetPort) ? targetPort : null;
                default -> null;
            };
        }

        private static Map<String, JsonNode> objectFields(JsonNode node) {
            if (node == null || !node.isObject()) {
                return Map.of();
            }
            Map<String, JsonNode> result = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                result.put(field.getKey(), field.getValue());
            }
            return result;
        }

        private static String firstText(JsonNode node, String... fields) {
            if (node == null || !node.isObject()) {
                return null;
            }
            for (String field : fields) {
                JsonNode value = node.get(field);
                if (value != null && value.isTextual() && StringUtils.hasText(value.textValue())) {
                    return value.textValue();
                }
            }
            return null;
        }
    }

    private BizException unsupportedOperator(String blockType) {
        return new BizException(ErrorCode.RENDER_FAILED,
                blockType + " 的操作符 OP 不支持");
    }
}
