package com.csg.ecard.messagecenter.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置。
 * <p>
 * 定义接口文档基础信息，实际访问路径由 springdoc 与 Knife4j 配置项控制。
 */
@Configuration
public class Knife4jConfig {

    /**
     * 创建消息中心 OpenAPI 文档元信息。
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI messageCenterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CSG E-Card Message Center API")
                        .description("电子卡消息中心后端基础框架接口文档")
                        .version("0.0.1")
                        .contact(new Contact().name("CSG"))
                        .license(new License().name("Internal")));
    }
}
