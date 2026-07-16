package com.csg.ecard.messagecenter.infrastructure.employee;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 员工中心接入配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.employee")
public class EmployeeProperties {

    /**
     * 员工来源：preset 本地预设；remote 内网员工接口。
     */
    private String mode = "preset";

    private Remote remote = new Remote();

    @Getter
    @Setter
    public static class Remote {

        /**
         * 内网公共服务基础地址，例如 http://public-jadp-service。
         */
        private String baseUrl;

        /**
         * 老接口上下文路径。
         */
        private String contextPath = "public-jadp-api";

        private Duration connectTimeout = Duration.ofSeconds(2);

        private Duration readTimeout = Duration.ofSeconds(5);

        /**
         * 员工信息本地缓存时长。
         */
        private Duration cacheTtl = Duration.ofMinutes(10);

        /**
         * 员工信息本地缓存最大条目数。
         */
        private int cacheMaxSize = 10000;
    }
}
