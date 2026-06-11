package com.csg.ecard.messagecenter.config.web;

import com.csg.ecard.messagecenter.framework.filter.RequestLogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web 层基础配置。
 * <p>
 * 注册请求日志过滤器，为所有接口补充 traceId、访问日志和请求结束清理动作。
 */
@Configuration
public class WebConfig {

    /**
     * 创建请求日志过滤器。
     *
     * @return 请求日志过滤器
     */
    @Bean
    public RequestLogFilter requestLogFilter() {
        return new RequestLogFilter();
    }

    /**
     * 注册请求日志过滤器到 Servlet 容器。
     *
     * @param requestLogFilter 请求日志过滤器
     * @return 过滤器注册信息
     */
    @Bean
    public FilterRegistrationBean<RequestLogFilter> requestLogFilterRegistration(RequestLogFilter requestLogFilter) {
        FilterRegistrationBean<RequestLogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(requestLogFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
