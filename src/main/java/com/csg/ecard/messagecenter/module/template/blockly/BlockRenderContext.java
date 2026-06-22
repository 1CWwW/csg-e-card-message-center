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
        String paramIdText = extraState.path("paramId").asText();
        String paramName = extraState.path("paramName").asText();
        Long paramId;
        try {
            paramId = Long.valueOf(paramIdText);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "场景参数节点paramId必须为有效ID字符串");
        }
        MsgSceneParam param = params.get(paramId);
        if (param == null || !sceneId.equals(param.getSceneId()) || !paramName.equals(param.getParamName())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板引用的参数不存在、已删除或不属于当前场景：" + paramName);
        }
        usedParams.add(paramName);
        JsonNode rawValue = values.get(paramName);
        boolean provided = values.containsKey(paramName);
        String formatted = valueValidator.validateAndFormat(param, rawValue, provided);
        BlocklyValueType type;
        try {
            type = BlocklyValueType.fromParamType(param.getParamType());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "场景参数类型不合法：" + param.getParamName());
        }
        if (!provided || rawValue == null || rawValue.isNull()) {
            return new BlocklyRenderValue(type, null);
        }
        Object value = switch (type) {
            case NUMBER -> new BigDecimal(formatted);
            case STRING, TIME -> formatted;
            case STRING_ARRAY -> toStringArray(rawValue);
            case NUMBER_ARRAY -> toNumberArray(rawValue);
            case BOOLEAN, STATEMENT -> throw new BizException(ErrorCode.PARAM_ERROR,
                    "场景参数类型不支持：" + param.getParamType());
        };
        return new BlocklyRenderValue(type, value);
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
