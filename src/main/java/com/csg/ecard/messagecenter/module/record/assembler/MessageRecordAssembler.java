package com.csg.ecard.messagecenter.module.record.assembler;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.config.message.MessageRecordProperties;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelTypeConfigDTO;
import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordDetailRow;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordDetailVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordListVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordSceneParamItemVO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息记录响应组装器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRecordAssembler {

    private static final Set<String> SAFE_CHANNEL_CONFIG_KEYS =
            Set.of("senderNumber", "senderEmail", "appId");

    private final ObjectMapper objectMapper;
    private final MessageRecordProperties recordProperties;

    /**
     * 补充列表中的枚举描述和操作标识。
     */
    public void enrichList(List<MessageRecordListVO> records) {
        records.forEach(item -> {
            item.setChannelTypeDesc(channelTypeDesc(item.getChannelType()));
            item.setSendStatusDesc(sendStatusDesc(item.getSendStatus()));
            item.setPriority(normalizePriority(item.getPriority()).getCode());
            item.setPriorityDesc(priorityDesc(item.getPriority()));
            MessageCallType callType = normalizeCallType(item.getCallType());
            item.setCallType(callType.getCode());
            item.setCallTypeDesc(callType.getDescription());
            item.setResendCount(normalizeResendCount(item.getResendCount()));
            item.setMaxResendCount(normalizeMaxResendCount(item.getMaxResendCount()));
            item.setCanResend(canResend(item.getSendStatus(),
                    item.getResendCount(), item.getMaxResendCount()));
        });
    }

    /**
     * 组装详情并容错解析历史场景参数。
     */
    public MessageRecordDetailVO toDetail(MessageRecordDetailRow row,
                                          List<MsgSceneParam> definitions) {
        MessageRecordDetailVO vo = new MessageRecordDetailVO();
        vo.setId(row.getId());
        vo.setMsgId(row.getMsgId());
        vo.setBizId(row.getBizId());
        vo.setSceneCode(row.getSceneCode());
        vo.setSceneName(row.getSceneName());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        vo.setUserId(row.getUserId());
        vo.setUserName(row.getUserName());
        vo.setUserOrgId(row.getUserOrgId());
        MessagePriority priority = normalizePriority(row.getPriority());
        vo.setPriority(priority.getCode());
        vo.setPriorityDesc(priority.getDesc());
        MessageCallType callType = normalizeCallType(row.getCallType());
        vo.setCallType(callType.getCode());
        vo.setCallTypeDesc(callType.getDescription());
        vo.setChannelId(row.getChannelId());
        vo.setChannelName(row.getChannelName());
        vo.setChannelType(row.getChannelType());
        vo.setChannelTypeDesc(channelTypeDesc(row.getChannelType()));
        vo.setChannelConfigSummary(parseSafeChannelConfig(row.getId(), row.getChannelTypeConfig()));
        vo.setTemplateId(row.getTemplateId());
        vo.setTemplateName(row.getTemplateName());
        vo.setTemplateChannelType(row.getTemplateChannelType());
        vo.setTemplateChannelTypeDesc(channelTypeDesc(row.getTemplateChannelType()));
        vo.setSendStatus(row.getSendStatus());
        vo.setSendStatusDesc(sendStatusDesc(row.getSendStatus()));
        vo.setResendCount(normalizeResendCount(row.getResendCount()));
        vo.setMaxResendCount(normalizeMaxResendCount(row.getMaxResendCount()));
        vo.setErrorMsg(row.getErrorMsg());
        vo.setErrorStack(row.getErrorStack());
        vo.setSendTime(row.getSendTime());
        vo.setCanResend(canResend(row.getSendStatus(),
                vo.getResendCount(), vo.getMaxResendCount()));
        vo.setMessageContent(row.getMessageContent());
        vo.setFullMessageContent(row.getMessageContent());
        vo.setSceneParamsRaw(row.getSceneParams());

        Map<String, Object> sceneParams = parseSceneParams(row.getId(), row.getSceneParams());
        vo.setSceneParams(sceneParams);
        vo.setSceneParamItems(buildSceneParamItems(sceneParams, definitions));
        return vo;
    }

    /**
     * 解析重发需要的历史场景参数。
     */
    public Map<String, Object> parseSceneParamsForResend(Long recordId, String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("场景参数JSON解析失败，无法重发");
        }
    }

    public String channelTypeDesc(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        try {
            return ChannelType.fromCode(code).getDesc();
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

    public String sendStatusDesc(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return switch (SendStatus.valueOf(status)) {
                case SUCCESS -> "发送成功";
                case FAILED -> "发送失败";
                case PENDING -> "待发送";
                case ACCEPTED -> "已受理";
            };
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    public MessagePriority normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return MessagePriority.NORMAL;
        }
        try {
            return MessagePriority.fromCode(priority);
        } catch (IllegalArgumentException ex) {
            return MessagePriority.NORMAL;
        }
    }

    public String priorityDesc(String priority) {
        return normalizePriority(priority).getDesc();
    }

    public MessageCallType normalizeCallType(String callType) {
        if (!StringUtils.hasText(callType)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "消息记录缺少原始调用方式");
        }
        try {
            return MessageCallType.fromCode(callType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "消息记录原始调用方式异常");
        }
    }

    public String callTypeDesc(String callType) {
        return normalizeCallType(callType).getDescription();
    }

    public int normalizeResendCount(Integer resendCount) {
        return resendCount == null ? 0 : Math.max(resendCount, 0);
    }

    public int normalizeMaxResendCount(Integer maxResendCount) {
        int configuredDefault = Math.max(recordProperties.getMaxResendCount(), 0);
        return maxResendCount == null ? configuredDefault : Math.max(maxResendCount, 0);
    }

    public boolean canResend(String sendStatus, Integer resendCount, Integer maxResendCount) {
        return SendStatus.FAILED.name().equals(sendStatus)
                && normalizeResendCount(resendCount) < normalizeMaxResendCount(maxResendCount);
    }

    private Map<String, Object> parseSceneParams(Long recordId, String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            log.warn("消息记录场景参数JSON解析失败，recordId={}, rawLength={}",
                    recordId, raw.length());
            return Collections.emptyMap();
        }
    }

    private List<MessageRecordSceneParamItemVO> buildSceneParamItems(
            Map<String, Object> values,
            List<MsgSceneParam> definitions) {
        List<MessageRecordSceneParamItemVO> items = new ArrayList<>();
        Set<String> definedNames = definitions.stream()
                .map(MsgSceneParam::getParamName)
                .collect(Collectors.toSet());
        for (MsgSceneParam definition : definitions) {
            Object value = values.get(definition.getParamName());
            items.add(toParamItem(definition.getParamName(),
                    definition.getParamLabel(),
                    definition.getParamType(),
                    value));
        }
        values.forEach((name, value) -> {
            if (!definedNames.contains(name)) {
                items.add(toParamItem(name, name, inferParamType(value), value));
            }
        });
        return items;
    }

    private MessageRecordSceneParamItemVO toParamItem(String name,
                                                       String label,
                                                       String type,
                                                       Object value) {
        MessageRecordSceneParamItemVO item = new MessageRecordSceneParamItemVO();
        item.setParamName(name);
        item.setParamLabel(label);
        item.setParamType(type);
        item.setParamTypeDesc(paramTypeDesc(type));
        item.setValue(value);
        item.setValueText(valueText(value));
        return item;
    }

    private String inferParamType(Object value) {
        if (value instanceof Boolean) {
            return ParamType.BOOLEAN.getCode();
        }
        if (value instanceof Number) {
            return ParamType.NUMBER.getCode();
        }
        if (value instanceof Collection<?> collection) {
            boolean allObjects = collection.stream().allMatch(Map.class::isInstance);
            if (allObjects) {
                return ParamType.OBJECT_ARRAY.getCode();
            }
            boolean allNumbers = collection.stream().allMatch(Number.class::isInstance);
            return allNumbers ? ParamType.NUMBER_ARRAY.getCode() : ParamType.STRING_ARRAY.getCode();
        }
        return value == null ? null : ParamType.STRING.getCode();
    }

    private String paramTypeDesc(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        try {
            return ParamType.fromCode(type).getDesc();
        } catch (IllegalArgumentException ex) {
            return type;
        }
    }

    private String valueText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
        if (value instanceof Map<?, ?> || value instanceof JsonNode) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException ex) {
                return String.valueOf(value);
            }
        }
        return String.valueOf(value);
    }

    private ChannelTypeConfigDTO parseSafeChannelConfig(Long recordId, String raw) {
        ChannelTypeConfigDTO summary = new ChannelTypeConfigDTO();
        if (!StringUtils.hasText(raw)) {
            return summary;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isObject()) {
                return summary;
            }
            if (SAFE_CHANNEL_CONFIG_KEYS.contains("senderNumber") && root.hasNonNull("senderNumber")) {
                summary.setSenderNumber(root.get("senderNumber").asText());
            }
            if (SAFE_CHANNEL_CONFIG_KEYS.contains("senderEmail") && root.hasNonNull("senderEmail")) {
                summary.setSenderEmail(root.get("senderEmail").asText());
            }
            if (SAFE_CHANNEL_CONFIG_KEYS.contains("appId") && root.hasNonNull("appId")) {
                summary.setAppId(root.get("appId").asText());
            }
            return summary;
        } catch (JsonProcessingException ex) {
            log.warn("消息记录渠道配置摘要解析失败，recordId={}, rawLength={}",
                    recordId, raw.length());
            return summary;
        }
    }
}
