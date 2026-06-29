package com.csg.ecard.messagecenter.common.utils;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 消息中心编码与格式校验工具。
 */
public final class MessageCenterValidator {

    private static final Pattern SCENE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");
    private static final Pattern PARAM_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]*$");
    private static final Set<String> PARAM_NAME_RESERVED_WORDS = Set.of(
            "true", "false", "null", "undefined", "if", "else",
            "for", "while", "return", "function", "var", "let", "const",
            "new", "this", "class", "switch", "case", "break", "continue",
            "try", "catch", "finally", "throw", "async", "await"
    );

    private MessageCenterValidator() {
    }

    /**
     * 校验场景编码格式。
     *
     * @param sceneCode 场景编码
     * @return 是否合法
     */
    public static boolean isValidSceneCode(String sceneCode) {
        return StringUtils.hasText(sceneCode)
                && sceneCode.length() <= MessageCenterConstants.SCENE_CODE_MAX_LENGTH
                && SCENE_CODE_PATTERN.matcher(sceneCode).matches();
    }

    /**
     * 校验参数名格式和保留字。
     *
     * @param paramName 参数名
     * @return 是否合法
     */
    public static boolean isValidParamName(String paramName) {
        return StringUtils.hasText(paramName)
                && paramName.length() <= MessageCenterConstants.PARAM_NAME_MAX_LENGTH
                && PARAM_NAME_PATTERN.matcher(paramName).matches()
                && !isReservedParamName(paramName);
    }

    /**
     * 判断参数名是否为保留字。
     *
     * @param paramName 参数名
     * @return 是否保留字
     */
    public static boolean isReservedParamName(String paramName) {
        return paramName != null && PARAM_NAME_RESERVED_WORDS.contains(paramName.toLowerCase());
    }

    /**
     * 校验模板名称。
     *
     * @param templateName 模板名称
     * @return 是否合法
     */
    public static boolean isValidTemplateName(String templateName) {
        return StringUtils.hasText(templateName)
                && templateName.length() <= MessageCenterConstants.TEMPLATE_NAME_MAX_LENGTH;
    }

    /**
     * 校验渠道名称。
     *
     * @param channelName 渠道名称
     * @return 是否合法
     */
    public static boolean isValidChannelName(String channelName) {
        return StringUtils.hasText(channelName)
                && channelName.length() <= MessageCenterConstants.CHANNEL_NAME_MAX_LENGTH;
    }

    /**
     * 校验描述长度，允许为空。
     *
     * @param description 描述
     * @return 是否合法
     */
    public static boolean isValidDescription(String description) {
        return !StringUtils.hasText(description)
                || description.length() <= MessageCenterConstants.DESCRIPTION_MAX_LENGTH;
    }

    public static void requireValidSceneCode(String sceneCode) {
        if (!isValidSceneCode(sceneCode)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "场景编码格式不正确");
        }
    }

    public static void requireValidParamName(String paramName) {
        if (!isValidParamName(paramName)) {
            throw new BizException(ErrorCode.PARAM_NAME_INVALID);
        }
    }

    public static void requireValidTemplateName(String templateName) {
        if (!isValidTemplateName(templateName)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板名称不能为空且长度不能超过50");
        }
    }

    public static void requireValidChannelName(String channelName) {
        if (!isValidChannelName(channelName)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "渠道名称不能为空且长度不能超过50");
        }
    }

    public static void requireValidDescription(String description) {
        if (!isValidDescription(description)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "描述长度不能超过200");
        }
    }
}
