package com.csg.ecard.messagecenter.common.enums;

import lombok.Getter;

/**
 * 全局错误码定义。
 * <p>
 * 错误码按接口层、业务层、基础设施层分类，供统一返回和全局异常处理复用。
 */
@Getter
public enum ErrorCode {

    SUCCESS("00000", "成功"),
    PARAM_ERROR("A0400", "请求参数错误"),
    UNAUTHORIZED("A0401", "未认证"),
    FORBIDDEN("A0403", "无访问权限"),
    NOT_FOUND("A0404", "资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "请求方法不支持"),
    CONFLICT("A0409", "请求冲突"),
    TOO_MANY_REQUESTS("A0429", "请求过于频繁"),
    BUSINESS_ERROR("B0001", "业务处理失败"),
    IDEMPOTENT_REJECTED("B0002", "重复请求"),
    INFRASTRUCTURE_ERROR("C0001", "基础设施异常"),
    DATABASE_ERROR("C0300", "数据库访问异常"),
    REDIS_ERROR("C0400", "Redis访问异常"),
    RABBITMQ_ERROR("C0500", "RabbitMQ访问异常"),
    XXL_JOB_ERROR("C0600", "XXL-Job执行异常"),
    SYSTEM_ERROR("C9999", "系统异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
