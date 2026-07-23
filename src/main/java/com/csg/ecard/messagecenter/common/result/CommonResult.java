package com.csg.ecard.messagecenter.common.result;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * REST 接口统一返回对象。
 *
 * @param <T> 响应数据类型
 */
@Getter
@Setter
@ToString
@Schema(description = "统一接口返回结构")
public class CommonResult<T> {

    @Schema(description = "业务状态码")
    private long code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T result;

    private CommonResult(long code, String message, T result) {
        this.code = code;
        this.message = message;
        this.result = result;
    }

    /**
     * 构造无业务数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> CommonResult<T> success() {
        return success(null);
    }

    /**
     * 构造带业务数据的成功响应。
     *
     * @param result 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> CommonResult<T> success(T result) {
        return new CommonResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), result);
    }

    /**
     * 按错误码枚举构造失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       响应数据类型
     * @return 失败响应
     */
    public static <T> CommonResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 按错误码枚举和自定义提示构造失败响应。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误提示
     * @param <T>       响应数据类型
     * @return 失败响应
     */
    public static <T> CommonResult<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode.getCode(), message, null);
    }

    /**
     * 按业务状态码和提示构造失败响应。
     *
     * @param code    业务状态码
     * @param message 错误提示
     * @param <T>     响应数据类型
     * @return 失败响应
     */
    public static <T> CommonResult<T> fail(long code, String message) {
        return fail(code, message, null);
    }

    /**
     * 按业务状态码、提示和扩展数据构造失败响应。
     *
     * @param code    业务状态码
     * @param message 错误提示
     * @param result  扩展响应数据
     * @param <T>     响应数据类型
     * @return 失败响应
     */
    public static <T> CommonResult<T> fail(long code, String message, T result) {
        return new CommonResult<>(code, message, result);
    }
}
