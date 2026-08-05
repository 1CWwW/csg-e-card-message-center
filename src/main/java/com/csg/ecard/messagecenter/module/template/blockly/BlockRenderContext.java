package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blockly 正文渲染上下文，集中维护示例值、参数使用顺序和循环上下文。
 */
public class BlockRenderContext {

    private final Long sceneId;
    private final Map<Long, MsgSceneParam> params;
    private final Map<String, JsonNode> values;
    private final SceneParamValueValidator valueValidator;
    private final Set<String> usedParams = new LinkedHashSet<>();

    /** 循环上下文栈，用于 forEach 渲染时传递当前元素值和类型。 */
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();

    public BlockRenderContext(Long sceneId,
                              Map<Long, MsgSceneParam> params,
                              Map<String, JsonNode> values,
                              SceneParamValueValidator valueValidator) {
        this.sceneId = sceneId;
        this.params = params == null ? Collections.emptyMap() : params;
        this.values = values == null ? Collections.emptyMap() : values;
        this.valueValidator = valueValidator;
    }

    /**
     * 解析并校验场景参数示例值。
     *
     * @param block 场景参数积木
     * @return 可直接参与正文拼接的渲染值
     */
    public BlocklyRenderValue resolveParam(JsonNode block) {
        JsonNode extraState = block.get("extraState");
        String paramName = extraState.path("paramName").asText();
        MsgSceneParam param = resolveParam(extraState, paramName);
        if (param == null || !sceneId.equals(param.getSceneId()) || !paramName.equals(param.getParamName())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板引用的参数不存在、已删除或不属于当前场景：" + paramName);
        }
        usedParams.add(paramName);
        JsonNode rawValue = values.get(paramName);
        boolean provided = values.containsKey(paramName);
        String formatted = valueValidator.validateAndFormat(param, rawValue, provided);
        BlocklyValueType type = resolveValueType(param);
        if (!provided || rawValue == null || rawValue.isNull()) {
            return new BlocklyRenderValue(type, null);
        }
        Object value = switch (type) {
            case NUMBER -> new BigDecimal(formatted);
            case BOOLEAN -> rawValue.booleanValue();
            case STRING, TIME -> formatted;
            case STRING_ARRAY -> toStringArray(rawValue);
            case NUMBER_ARRAY -> toNumberArray(rawValue);
            case OBJECT_ARRAY -> toObjectArray(rawValue);
            case OBJECT, STATEMENT -> throw new BizException(ErrorCode.PARAM_ERROR,
                    "场景参数类型不支持：" + param.getParamType());
        };
        return new BlocklyRenderValue(type, value);
    }

    /**
     * 解析比较节点参数，允许可选参数以空值参与比较短路。
     *
     * @param block 场景参数积木
     * @return 比较节点运行时值
     */
    public BlocklyRenderValue resolveCompareParam(JsonNode block) {
        return resolveNullableScalarParam(block);
    }

    /**
     * 解析金额格式化参数，允许可选数字参数为空。
     *
     * @param block 场景参数积木
     * @return 金额格式化运行时值
     */
    public BlocklyRenderValue resolveAmountParam(JsonNode block) {
        return resolveNullableScalarParam(block);
    }

    private BlocklyRenderValue resolveNullableScalarParam(JsonNode block) {
        JsonNode extraState = block.get("extraState");
        String paramName = extraState.path("paramName").asText();
        MsgSceneParam param = resolveParam(extraState, paramName);
        if (param == null || !sceneId.equals(param.getSceneId()) || !paramName.equals(param.getParamName())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板引用的参数不存在、已删除或不属于当前场景：" + paramName);
        }
        usedParams.add(paramName);
        JsonNode rawValue = values.get(paramName);
        boolean provided = values.containsKey(paramName);
        BlocklyValueType type = resolveValueType(param);
        if (isEmptyNullableValue(rawValue, provided)) {
            if (Integer.valueOf(1).equals(param.getIsRequired())) {
                if (provided && rawValue != null && rawValue.isTextual()) {
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "必填参数值为空：" + param.getParamName());
                }
                valueValidator.validateAndFormat(param, rawValue, provided);
            }
            return new BlocklyRenderValue(type, null);
        }
        if (type == BlocklyValueType.NUMBER) {
            Object value = rawValue.isNumber() ? rawValue.decimalValue() : rawValue;
            return new BlocklyRenderValue(type, value);
        }
        if (type == BlocklyValueType.TIME) {
            Object value = rawValue.isTextual() ? rawValue.textValue() : rawValue;
            return new BlocklyRenderValue(type, value);
        }
        return resolveParam(block);
    }

    /**
     * 获取场景参数原始预览值，用于链式格式化节点区分输入值和格式模板。
     */
    public String resolveRawParamText(JsonNode block) {
        JsonNode extraState = block.get("extraState");
        String paramName = extraState.path("paramName").asText();
        MsgSceneParam param = resolveParam(extraState, paramName);
        if (param == null || !sceneId.equals(param.getSceneId()) || !paramName.equals(param.getParamName())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板引用的参数不存在、已删除或不属于当前场景：" + paramName);
        }
        usedParams.add(paramName);
        JsonNode rawValue = values.get(paramName);
        boolean provided = values.containsKey(paramName);
        if (!provided) {
            return missingValueText(param, false);
        }
        if (rawValue == null || rawValue.isNull()) {
            return "";
        }
        if (rawValue.isTextual()) {
            return rawValue.textValue();
        }
        return rawValue.asText();
    }

    private MsgSceneParam resolveParam(JsonNode extraState, String paramName) {
        String paramIdText = extraState.path("paramId").asText();
        if (paramIdText != null && !paramIdText.isBlank()) {
            try {
                return params.get(Long.valueOf(paramIdText));
            } catch (NumberFormatException ex) {
                throw new BizException(ErrorCode.PARAM_ERROR, "场景参数节点paramId必须为有效ID字符串");
            }
        }
        return params.values().stream()
                .filter(param -> paramName.equals(param.getParamName()))
                .findFirst()
                .orElse(null);
    }

    private BlocklyValueType resolveValueType(MsgSceneParam param) {
        try {
            return BlocklyValueType.fromParamType(param.getParamType());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "场景参数类型不合法：" + param.getParamName());
        }
    }

    private boolean isEmptyNullableValue(JsonNode rawValue, boolean provided) {
        return !provided
                || rawValue == null
                || rawValue.isNull()
                || (rawValue.isTextual() && rawValue.textValue().isEmpty());
    }

    private String missingValueText(MsgSceneParam param, boolean provided) {
        if (Integer.valueOf(1).equals(param.getIsRequired())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    (provided ? "必填参数值为空：" : "必填参数未提供：") + param.getParamName());
        }
        return "";
    }

    private List<String> toStringArray(JsonNode rawValue) {
        return java.util.stream.StreamSupport.stream(rawValue.spliterator(), false)
                .map(JsonNode::textValue)
                .toList();
    }

    private List<BigDecimal> toNumberArray(JsonNode rawValue) {
        return java.util.stream.StreamSupport.stream(rawValue.spliterator(), false)
                .map(JsonNode::decimalValue)
                .toList();
    }

    /** 进入循环上下文，设置当前循环元素值和类型。 */
    private List<JsonNode> toObjectArray(JsonNode rawValue) {
        return java.util.stream.StreamSupport.stream(rawValue.spliterator(), false)
                .toList();
    }

    public void pushLoopContext(BlocklyValueType itemType, Object itemValue) {
        loopStack.push(new LoopContext(itemType, itemValue));
    }

    /** 退出循环上下文。 */
    public void popLoopContext() {
        if (!loopStack.isEmpty()) {
            loopStack.pop();
        }
    }

    public boolean hasLoopContext() {
        return !loopStack.isEmpty();
    }

    /** 获取当前循环元素类型，无循环上下文时返回 null。 */
    public BlocklyValueType currentLoopItemType() {
        return loopStack.isEmpty() ? null : loopStack.peek().itemType;
    }

    /** 获取当前循环元素值。 */
    public Object currentLoopItemValue() {
        return loopStack.isEmpty() ? null : loopStack.peek().itemValue;
    }

    public List<String> getUsedParams() {
        return List.copyOf(usedParams);
    }

    public List<String> getWarnings() {
        return values.keySet().stream()
                .filter(name -> !usedParams.contains(name))
                .map(name -> "参数未被模板引用，已忽略：" + name)
                .toList();
    }

    private record LoopContext(BlocklyValueType itemType, Object itemValue) {
    }
}
