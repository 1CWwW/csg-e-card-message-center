package com.csg.ecard.messagecenter.config.secret;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 遍历配置源并用解密后的属性覆盖原始密文。
 */
public abstract class PropertySecretContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * 按属性名解码配置值。
     *
     * @param key   属性名
     * @param input 原始属性值
     * @return 解码后的属性值
     */
    public abstract String decode(String key, String input);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        List<PropertySource<?>> propertySources = new ArrayList<>();
        environment.getPropertySources().forEach(propertySources::add);

        for (PropertySource<?> propertySource : propertySources) {
            Map<String, Object> propertyOverrides = new LinkedHashMap<>();
            decodeProperty(environment, propertySource, propertyOverrides);
            if (!propertyOverrides.isEmpty()) {
                PropertySource<?> decodedProperties = new MapPropertySource(
                        "decoded " + propertySource.getName(),
                        propertyOverrides
                );
                environment.getPropertySources().addBefore(propertySource.getName(), decodedProperties);
            }
        }
    }

    private void decodeProperty(ConfigurableEnvironment environment,
                                PropertySource<?> source,
                                Map<String, Object> propertyOverrides) {
        if (!(source instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
            return;
        }
        for (String key : enumerablePropertySource.getPropertyNames()) {
            Object rawValue = source.getProperty(key);
            if (rawValue instanceof String stringValue) {
                String resolvedValue = environment.resolvePlaceholders(stringValue);
                String decodedValue = decode(key, resolvedValue);
                if (!Objects.equals(decodedValue, resolvedValue)) {
                    propertyOverrides.put(key, decodedValue);
                }
            }
        }
    }
}
