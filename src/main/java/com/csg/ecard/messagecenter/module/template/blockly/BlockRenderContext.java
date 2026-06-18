package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blockly 正文渲染上下文，集中维护示例值和参数使用顺序。
 */
public class BlockRenderContext {

    private final Long sceneId;
    private final Map<Long, MsgSceneParam> params;
    private final Map<String, JsonNode> values;
    private final SceneParamValueValidator valueValidator;
    private final Set<String> usedParams = new LinkedHashSet<>();

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
     * @return 可直接参与正文拼接的字符串
     */
    public String resolveParam(JsonNode block) {
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
        return valueValidator.validateAndFormat(param, values.get(paramName), values.containsKey(paramName));
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
}
