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

    SUCCESS("00000", "成功"),
    PARAM_ERROR("A0400", "参数错误"),
    UNAUTHORIZED("A0401", "未认证"),
    FORBIDDEN("A0403", "无访问权限"),
    NOT_FOUND("A0404", "资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "请求方法不支持"),
    CONFLICT("A0409", "请求冲突"),
    TOO_MANY_REQUESTS("A0429", "请求过于频繁"),
    BUSINESS_ERROR("B0001", "业务处理失败"),
    DATA_NOT_FOUND("B0002", "数据不存在"),
    DATA_DUPLICATE("B0003", "数据重复"),
    STATUS_NOT_ALLOWED("B0004", "当前状态不允许操作"),
    DELETE_NOT_ALLOWED("B0005", "当前数据不允许删除"),
    SCENE_CODE_INVALID("B0101", "场景编码格式不合法"),
    PARAM_NAME_INVALID("B0102", "参数名格式不合法"),
    CHANNEL_TYPE_INVALID("B0103", "渠道类型不合法"),
    TEMPLATE_EMPTY("B0201", "模板内容为空"),
    RENDER_FAILED("B0202", "模板渲染失败"),
    CHANNEL_SEND_FAILED("B0301", "渠道发送失败"),
    IDEMPOTENT_REPEAT("B0401", "重复请求"),
    IDEMPOTENT_REJECTED("B0401", "重复请求"),
    INFRASTRUCTURE_ERROR("C0001", "基础设施异常"),
    EXTERNAL_SERVICE_ERROR("C0002", "外部服务异常"),
    DATABASE_ERROR("C0300", "数据库访问异常"),
    REDIS_ERROR("C0400", "Redis访问异常"),
    RABBITMQ_ERROR("C0500", "RabbitMQ访问异常"),
    XXL_JOB_ERROR("C0600", "XXL-Job执行异常"),
    SYSTEM_ERROR("C9999", "系统异常");

    private final String code;
    private final String desc;

    ErrorCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据错误码获取枚举，非法 code 统一抛出参数异常。
     *
     * @param code 错误码
     * @return 错误码枚举
     */
    public static ErrorCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
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
