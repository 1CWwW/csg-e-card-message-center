package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

/**
 * 场景参数示例值校验与正文输出转换器。
 */
@Component
public class SceneParamValueValidator {

    private static final int REQUIRED = 1;

    /**
     * 按场景参数定义校验示例值，并转换为正文字符串。
     *
     * @param param    场景参数定义
     * @param value    示例值
     * @param provided 请求是否提供该参数名
     * @return 参数正文值，非必填参数未提供时返回空字符串
     */
    public String validateAndFormat(MsgSceneParam param, JsonNode value, boolean provided) {
        ParamType type = requireParamType(param);
        if (!provided) {
            return missingValue(param);
        }
        if (value == null || value.isNull()) {
            return nullValue(param);
        }
        return switch (type) {
            case STRING -> formatString(param, value);
            case NUMBER -> formatNumber(param, value);
            case BOOLEAN -> formatBoolean(param, value);
            case TIME -> formatTime(param, value);
            case STRING_ARRAY -> formatStringArray(param, value);
            case NUMBER_ARRAY -> formatNumberArray(param, value);
            case OBJECT_ARRAY -> formatObjectArray(param, value);
        };
    }

    private String formatString(MsgSceneParam param, JsonNode value) {
        if (!value.isTextual()) {
            throw typeError(param, "JSON字符串");
        }
        String text = value.textValue();
        if (text.isEmpty() && isRequired(param)) {
            throw emptyValueError(param);
        }
        return text;
    }

    private String formatNumber(MsgSceneParam param, JsonNode value) {
        if (!value.isNumber()) {
            throw typeError(param, "JSON数字");
        }
        return value.decimalValue().toPlainString();
    }

    private String formatBoolean(MsgSceneParam param, JsonNode value) {
        if (!value.isBoolean()) {
            throw typeError(param, "JSON布尔值");
        }
        return Boolean.toString(value.booleanValue());
    }

    private String formatTime(MsgSceneParam param, JsonNode value) {
        if (!value.isTextual() && !value.isIntegralNumber()) {
            throw invalidTimeError(param);
        }
        try {
            return TemplateTimeFormatter.format(value.asText(),
                    TemplateTimeFormatter.BLOCKLY_DEFAULT_PATTERN);
        } catch (DateTimeException ex) {
            throw invalidTimeError(param);
        }
    }

    private BizException invalidTimeError(MsgSceneParam param) {
        return new BizException(ErrorCode.PARAM_ERROR,
                param.getParamName() + "的值不是有效日期，请检查后重试");
    }

    private String formatStringArray(MsgSceneParam param, JsonNode value) {
        if (!value.isArray()) {
            throw typeError(param, "JSON字符串数组");
        }
        if (value.isEmpty()) {
            return emptyArray(param);
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw typeError(param, "JSON字符串数组");
            }
            items.add(item.textValue());
        }
        return String.join("，", items);
    }

    private String formatNumberArray(MsgSceneParam param, JsonNode value) {
        if (!value.isArray()) {
            throw typeError(param, "JSON数字数组");
        }
        if (value.isEmpty()) {
            return emptyArray(param);
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isNumber()) {
                throw typeError(param, "JSON数字数组");
            }
            BigDecimal number = item.decimalValue();
            items.add(number.toPlainString());
        }
        return String.join("，", items);
    }

    private String formatObjectArray(MsgSceneParam param, JsonNode value) {
        if (!value.isArray()) {
            throw typeError(param, "JSON对象数组");
        }
        if (value.isEmpty()) {
            return emptyArray(param);
        }
        for (JsonNode item : value) {
            if (!item.isObject()) {
                throw typeError(param, "JSON对象数组");
            }
            item.fields().forEachRemaining(field -> {
                JsonNode fieldValue = field.getValue();
                if (fieldValue != null
                        && !fieldValue.isNull()
                        && !fieldValue.isTextual()
                        && !fieldValue.isNumber()
                        && !fieldValue.isBoolean()) {
                    throw typeError(param, "字段值为字符串、数字、布尔值或null的JSON对象数组");
                }
            });
        }
        return "";
    }

    private ParamType requireParamType(MsgSceneParam param) {
        try {
            return ParamType.fromCode(param.getParamType());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "场景参数类型不合法：" + param.getParamName());
        }
    }

    private String missingValue(MsgSceneParam param) {
        if (isRequired(param)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "必填参数未提供：" + param.getParamName());
        }
        return "";
    }

    private String nullValue(MsgSceneParam param) {
        if (isRequired(param)) {
            throw emptyValueError(param);
        }
        return "";
    }

    private String emptyArray(MsgSceneParam param) {
        if (isRequired(param)) {
            throw emptyValueError(param);
        }
        return "";
    }

    private boolean isRequired(MsgSceneParam param) {
        return Integer.valueOf(REQUIRED).equals(param.getIsRequired());
    }

    private BizException emptyValueError(MsgSceneParam param) {
        return new BizException(ErrorCode.PARAM_ERROR,
                "必填参数值为空：" + param.getParamName());
    }

    private BizException typeError(MsgSceneParam param, String expected) {
        return new BizException(ErrorCode.PARAM_ERROR,
                "参数" + param.getParamName() + "必须是" + expected);
    }
}
