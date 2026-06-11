package com.csg.ecard.messagecenter.common.result;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.MDC;

import java.time.LocalDateTime;

/**
 * REST 接口统一返回对象。
 * <p>
 * Controller 层应统一返回该结构，确保成功、失败、链路追踪 ID 和响应时间字段一致。
 *
 * @param <T> 响应数据类型
 */
@Getter
@Setter
@ToString
@Schema(description = "统一接口返回结构")
public class ApiResult<T> {

    @Schema(description = "业务状态码")
    private String code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "响应时间")
    private LocalDateTime timestamp;

    private ApiResult(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        // traceId 由请求日志过滤器写入 MDC，便于调用方和服务日志关联排查。
        this.traceId = MDC.get("traceId");
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 构造无业务数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /**
     * 构造带业务数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 按错误码枚举构造失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
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
    public static <T> ApiResult<T> fail(ErrorCode errorCode, String message) {
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
    public static <T> ApiResult<T> fail(String code, String message) {
        return fail(code, message, null);
    }

    /**
     * 按业务状态码、提示和扩展数据构造失败响应。
     *
     * @param code    业务状态码
     * @param message 错误提示
     * @param data    扩展响应数据
     * @param <T>     响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResult<T> fail(String code, String message, T data) {
        return new ApiResult<>(code, message, data);
    }
}
