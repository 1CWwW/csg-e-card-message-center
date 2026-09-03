package com.csg.ecard.messagecenter.module.channel.validator;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelTypeConfigDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 渠道类型配置校验器。
 */
@Component
public class ChannelTypeConfigValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final String MOBILE_ERROR_MESSAGE = "请输入正确的手机号码";

    /**
     * 按渠道类型校验并返回可入库的配置。
     *
     * @param channelType 渠道类型
     * @param config      类型配置
     * @return 只包含有效字段的配置
     */
    public Map<String, String> validateAndNormalize(ChannelType channelType, ChannelTypeConfigDTO config) {
        ChannelTypeConfigDTO safeConfig = config == null ? new ChannelTypeConfigDTO() : config;
        return switch (channelType) {
            case SMS -> validateSms(safeConfig);
            case EMAIL -> validateEmail(safeConfig);
            case ELINK -> validateElink(safeConfig);
            case IN_APP -> validateInApp(safeConfig);
        };
    }

    private Map<String, String> validateSms(ChannelTypeConfigDTO config) {
        String senderNumber = trim(config.getSenderNumber());
        if (!StringUtils.hasText(senderNumber) || !MOBILE_PATTERN.matcher(senderNumber).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MOBILE_ERROR_MESSAGE);
        }
        rejectText(config.getAppId(), "短信渠道不允许配置应用ID");
        return mapOf("senderNumber", senderNumber);
    }

    private Map<String, String> validateEmail(ChannelTypeConfigDTO config) {
        String senderEmail = trim(config.getSenderEmail());
        requireText(senderEmail, "邮件发送邮箱不能为空");
        requireMaxLength(senderEmail, 128, "邮件发送邮箱长度不能超过128");
        if (!EMAIL_PATTERN.matcher(senderEmail).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮件发送邮箱格式不正确");
        }
        rejectText(config.getAppId(), "邮件渠道不允许配置应用ID");
        return mapOf("senderEmail", senderEmail);
    }

    private Map<String, String> validateElink(ChannelTypeConfigDTO config) {
        String appId = trim(config.getAppId());
        requireText(appId, "eLink应用ID不能为空");
        requireMaxLength(appId, 64, "eLink应用ID长度不能超过64");
        return mapOf("appId", appId);
    }

    private Map<String, String> validateInApp(ChannelTypeConfigDTO config) {
        rejectText(config.getAppId(), "站内信渠道不允许配置应用ID");
        return Map.of();
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private void rejectText(String value, String message) {
        if (StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private void requireMaxLength(String value, int maxLength, String message) {
        if (value.length() > maxLength) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Map<String, String> mapOf(String key, String value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }
}
