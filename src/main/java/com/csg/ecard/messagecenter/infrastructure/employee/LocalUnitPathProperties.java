package com.csg.ecard.messagecenter.infrastructure.employee;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地单位层级配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.local-unit-path")
public class LocalUnitPathProperties {

    /**
     * 单位父级映射，key 为当前单位ID，value 为父级单位ID。
     */
    private Map<String, String> parentByUnit = new LinkedHashMap<>();

    /**
     * 单位路径最大向上查找层级，避免配置成环时无限循环。
     */
    private int maxDepth = 20;
}
