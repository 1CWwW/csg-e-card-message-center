package com.csg.ecard.messagecenter.config.web;

import com.csg.ecard.messagecenter.framework.interceptor.MessageCenterPermissionInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 消息中心后台接口权限配置。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "message-center.permission",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PermissionWebConfig implements WebMvcConfigurer {

    private final MessageCenterPermissionInterceptor permissionInterceptor;

    public PermissionWebConfig(MessageCenterPermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    /**
     * 为消息中心后台管理接口注册菜单权限拦截器。
     *
     * @param registry MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/msg/**");
    }
}
