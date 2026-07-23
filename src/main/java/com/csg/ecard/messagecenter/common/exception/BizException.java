package com.csg.ecard.messagecenter.common.exception;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 用于主动中断业务流程，并由全局异常处理器转换为统一返回结构。
 */
@Getter
public class BizException extends RuntimeException {

    private final long code;

    /**
     * 使用标准错误码创建业务异常。
     *
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用标准错误码和自定义提示创建业务异常。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误提示
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义错误码和提示创建业务异常。
     *
     * @param code    业务状态码
     * @param message 错误提示
     */
    public BizException(long code, String message) {
        super(message);
        this.code = code;
    }
}
