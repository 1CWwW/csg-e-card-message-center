package com.csg.ecard.messagecenter.config.message;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 消息发送外部平台配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.message-send")
public class MessageSendProperties {

    private Sms sms = new Sms();
    private Email email = new Email();
    private Elink elink = new Elink();
    private InApp inApp = new InApp();

    @Getter
    @Setter
    public static class Sms {
        private boolean enabled;
        private String xxptType = "NANWANG";
        private String baseUrl;
        private String authorization;
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(10);
    }

    @Getter
    @Setter
    public static class Email {
        private boolean enabled;
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String protocol = "smtp";
        private boolean auth = true;
        private boolean starttlsEnable;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(10);
    }

    @Getter
    @Setter
    public static class Elink {
        private boolean enabled;
        private String baseUrl;
        private String tokenPath;
        private String sendMsgPath;
        private String tokenUrl;
        private String sendUrl;
        private String appId;
        private String secret;
        private String agentId;
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(5);
    }

    @Getter
    @Setter
    public static class InApp {
        private boolean enabled;
        private String notificationUrl;
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(5);
    }
}
