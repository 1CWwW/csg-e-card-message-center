package com.csg.ecard.messagecenter.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.csg.ecard.messagecenter.common.constant.CommonConstants;
import com.csg.ecard.messagecenter.framework.context.CurrentUserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 基础配置。
 * <p>
 * 提供达梦数据库分页插件和实体公共字段自动填充能力，后续业务 Mapper 可直接复用。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器。
     * <p>
     * 分页插件显式指定 {@link DbType#DM}，避免按默认数据库方言生成不兼容达梦的分页 SQL。
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.DM));
        return interceptor;
    }

    /**
     * 注册公共字段自动填充处理器。
     * <p>
     * 新增时填充创建/更新时间、创建/更新人、逻辑删除标识；更新时刷新更新时间和更新人。
     *
     * @return MyBatis-Plus 元对象填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                String operator = CurrentUserContext.getUserIdOrDefault(CommonConstants.DEFAULT_OPERATOR);
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "createBy", String.class, operator);
                strictInsertFill(metaObject, "updateBy", String.class, operator);
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                strictUpdateFill(metaObject, "updateBy", String.class,
                        // 当前未接入认证系统，未设置上下文时使用 system 作为审计占位。
                        CurrentUserContext.getUserIdOrDefault(CommonConstants.DEFAULT_OPERATOR));
            }
        };
    }
}
