package com.csg.ecard.messagecenter.config.message;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息记录业务配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.message-record")
public class MessageRecordProperties {

    /**
     * 单条消息记录允许手动重发的默认最大次数。
     */
    private int maxResendCount = 5;
}
