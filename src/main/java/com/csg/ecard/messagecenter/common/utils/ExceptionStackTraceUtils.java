package com.csg.ecard.messagecenter.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常堆栈文本转换工具。
 */
public final class ExceptionStackTraceUtils {

    private static final int MAX_LENGTH = 65_535;
    private static final String TRUNCATED_SUFFIX = "\n...异常堆栈已截断";

    private ExceptionStackTraceUtils() {
    }

    /**
     * 获取包含 cause 链的异常堆栈，转换失败时返回简短异常信息。
     *
     * @param throwable 异常对象
     * @return 异常堆栈；异常为空时返回 null
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        try {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            String stackTrace = writer.toString();
            if (stackTrace.length() <= MAX_LENGTH) {
                return stackTrace;
            }
            int contentLength = MAX_LENGTH - TRUNCATED_SUFFIX.length();
            return stackTrace.substring(0, Math.max(contentLength, 0)) + TRUNCATED_SUFFIX;
        } catch (RuntimeException ex) {
            String message = throwable.getMessage();
            return throwable.getClass().getName()
                    + (message == null || message.isBlank() ? "" : ": " + message);
        }
    }
}
