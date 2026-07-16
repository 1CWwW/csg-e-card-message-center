package com.csg.ecard.messagecenter.infrastructure.organization;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 组织架构接入配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.organization")
public class OrganizationProperties {

    /**
     * 组织来源：preset 本地预设；remote 内网组织接口。
     */
    private String mode = "preset";

    private Preset preset = new Preset();

    private Remote remote = new Remote();

    private Tree tree = new Tree();

    @Getter
    @Setter
    public static class Preset {

        /**
         * 本地预设单位名称，key 为单位ID，value 为展示名称。
         */
        private Map<String, String> nameByUnit = new LinkedHashMap<>();
    }

    @Getter
    @Setter
    public static class Remote {

        /**
         * 内网组织服务基础地址，例如 http://public-jadp-service。
         */
        private String baseUrl;

        /**
         * 老接口上下文路径。
         */
        private String contextPath = "public-jadp-api";

        private Duration connectTimeout = Duration.ofSeconds(2);

        private Duration readTimeout = Duration.ofSeconds(5);

        /**
         * 全量组织数据本地缓存时长。
         */
        private Duration cacheTtl = Duration.ofMinutes(10);
    }

    @Getter
    @Setter
    public static class Tree {

        /**
         * 父节点直接子节点缓存时长。
         */
        private Duration childrenCacheTtl = Duration.ofMinutes(5);

        /**
         * 组织搜索最大返回数量。
         */
        private int searchLimit = 50;

        /**
         * 批量回显允许的最大组织数量。
         */
        private int resolveLimit = 200;
    }
}
