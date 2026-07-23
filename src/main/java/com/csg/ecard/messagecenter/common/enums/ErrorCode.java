package com.csg.ecard.messagecenter.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 全局错误码定义。
 * <p>
 * 错误码供统一返回结构和业务异常复用，新增业务能力应优先复用这里的标准错误码。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0L, "成功"),
    PARAM_ERROR(10400L, "参数错误"),
    UNAUTHORIZED(10401L, "未认证"),
    FORBIDDEN(10403L, "无访问权限"),
    NOT_FOUND(10404L, "资源不存在"),
    METHOD_NOT_ALLOWED(10405L, "请求方法不支持"),
    CONFLICT(10409L, "请求冲突"),
    TOO_MANY_REQUESTS(10429L, "请求过于频繁"),
    BUSINESS_ERROR(20001L, "业务处理失败"),
    DATA_NOT_FOUND(20002L, "数据不存在"),
    DATA_DUPLICATE(20003L, "数据重复"),
    STATUS_NOT_ALLOWED(20004L, "当前状态不允许操作"),
    DELETE_NOT_ALLOWED(20005L, "当前数据不允许删除"),
    SCENE_CODE_INVALID(20101L, "场景编码格式不合法"),
    PARAM_NAME_INVALID(20102L, "参数名格式不合法"),
    CHANNEL_TYPE_INVALID(20103L, "渠道类型不合法"),
    TEMPLATE_EMPTY(20201L, "模板内容为空"),
    RENDER_FAILED(20202L, "模板渲染失败"),
    CHANNEL_SEND_FAILED(20301L, "渠道发送失败"),
    IDEMPOTENT_REPEAT(20401L, "重复请求"),
    IDEMPOTENT_REJECTED(20401L, "重复请求"),
    INFRASTRUCTURE_ERROR(30001L, "基础设施异常"),
    EXTERNAL_SERVICE_ERROR(30002L, "外部服务异常"),
    DATABASE_ERROR(30300L, "数据库访问异常"),
    REDIS_ERROR(30400L, "Redis访问异常"),
    RABBITMQ_ERROR(30500L, "RabbitMQ访问异常"),
    XXL_JOB_ERROR(30600L, "XXL-Job执行异常"),
    SYSTEM_ERROR(39999L, "系统异常");

    private final long code;
    private final String desc;

    ErrorCode(long code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据错误码获取枚举，非法 code 统一抛出参数异常。
     *
     * @param code 错误码
     * @return 错误码枚举
     */
    public static ErrorCode fromCode(long code) {
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知错误码: " + code));
    }

    /**
     * 兼容已有统一返回和业务异常对 message 字段的调用。
     *
     * @return 错误描述
     */
    public String getMessage() {
        return desc;
    }
}
