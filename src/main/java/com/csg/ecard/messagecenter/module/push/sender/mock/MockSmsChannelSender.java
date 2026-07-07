package com.csg.ecard.messagecenter.module.push.sender.mock;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 短信本地模拟发送器。
 */
@Component
@ConditionalOnProperty(prefix = "message.sender", name = "mode", havingValue = "mock")
public class MockSmsChannelSender extends AbstractMockChannelSender {

    public MockSmsChannelSender(MockSenderProperties properties) {
        super(properties);
    }

    @Override
    public String channelType() {
        return ChannelType.SMS.getCode();
    }
}
