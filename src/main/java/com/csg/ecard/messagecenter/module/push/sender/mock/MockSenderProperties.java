package com.csg.ecard.messagecenter.module.push.sender.mock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地消息发送模拟配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "message.sender.mock")
public class MockSenderProperties {

    private MockSendResult defaultResult = MockSendResult.SUCCESS;
    private long delayMs = 100L;
    private Map<String, MockSendResult> resultByChannel = new LinkedHashMap<>();

    /**
     * 获取指定渠道的模拟结果。
     *
     * @param channelType 渠道类型
     * @return 模拟结果
     */
    public MockSendResult resolveResult(String channelType) {
        MockSendResult result = resultByChannel.get(channelType);
        return result == null ? defaultResult : result;
    }

}
