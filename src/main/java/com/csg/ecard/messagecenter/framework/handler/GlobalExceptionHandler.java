package com.csg.ecard.messagecenter.framework.handler;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 统一接管 Controller 抛出的业务异常、参数校验异常、基础设施异常和兜底系统异常，
 * 确保接口失败场景也返回统一结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务主动抛出的异常。
     *
     * @param ex 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public CommonResult<Void> handleBizException(BizException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return CommonResult.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理请求体 Bean Validation 校验失败。
     *
     * @param ex 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return CommonResult.fail(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理表单或查询参数绑定失败。
     *
     * @param ex 参数绑定异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return CommonResult.fail(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理方法参数上的 Jakarta Validation 校验失败。
     *
     * @param ex 约束校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(item -> item.getPropertyPath() + " " + item.getMessage())
                .collect(Collectors.joining("; "));
        return CommonResult.fail(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求缺参、类型不匹配、JSON 不合法等客户端请求错误。
     *
     * @param ex 请求异常
     * @return 统一失败响应
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return CommonResult.fail(ErrorCode.PARAM_ERROR, ex.getMessage());
    }

    /**
     * 处理 JSON 请求体格式错误，避免向前端暴露底层解析异常。
     *
     * @param ex JSON 解析异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid JSON request body: {}", ex.getMessage());
        InvalidFormatException invalidFormatException = findCause(ex, InvalidFormatException.class);
        InputCoercionException inputCoercionException = findCause(ex, InputCoercionException.class);
        boolean numericOutOfRange = invalidFormatException != null
                && isNumericType(invalidFormatException.getTargetType());
        numericOutOfRange = numericOutOfRange || inputCoercionException != null
                && isNumericType(inputCoercionException.getTargetType());
        if (numericOutOfRange) {
            JsonMappingException mappingException = findCause(ex, JsonMappingException.class);
            return CommonResult.fail(ErrorCode.PARAM_ERROR,
                    resolveJsonFieldName(mappingException) + "数值超出允许范围");
        }
        return CommonResult.fail(ErrorCode.PARAM_ERROR, "请求体格式不正确");
    }

    /**
     * 处理 HTTP 方法不支持。
     *
     * @param ex 请求方法异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public CommonResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return CommonResult.fail(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    /**
     * 处理数据库访问异常。
     *
     * @param ex 数据访问异常
     * @return 统一失败响应
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleDataAccessException(DataAccessException ex) {
        log.error("Database exception", ex);
        return CommonResult.fail(ErrorCode.DATABASE_ERROR);
    }

    /**
     * 兜底处理未分类异常。
     *
     * @param ex 未处理异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return CommonResult.fail(ErrorCode.SYSTEM_ERROR);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }

    private String resolveJsonFieldName(JsonMappingException ex) {
        if (ex == null || ex.getPath().isEmpty()) {
            return "字段";
        }
        String fieldName = ex.getPath().get(ex.getPath().size() - 1).getFieldName();
        return fieldName == null ? "字段" : fieldName;
    }

    private boolean isNumericType(Class<?> targetType) {
        return targetType != null && (Number.class.isAssignableFrom(targetType)
                || targetType == byte.class
                || targetType == short.class
                || targetType == int.class
                || targetType == long.class
                || targetType == float.class
                || targetType == double.class);
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            Throwable cause = current.getCause();
            current = cause == current ? null : cause;
        }
        return null;
    }
}
