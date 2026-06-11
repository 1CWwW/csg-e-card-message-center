package com.csg.ecard.messagecenter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 电子卡消息中心后端启动入口。
 * <p>
 * 当前工程为基础框架层，开启 Spring Boot 自动配置、定时能力以及 MyBatis Mapper 扫描。
 */
@EnableScheduling
@MapperScan("com.csg.ecard.messagecenter.**.mapper")
@SpringBootApplication
public class CsgECardMessageCenterApplication {

    /**
     * 应用启动方法。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CsgECardMessageCenterApplication.class, args);
    }
}
