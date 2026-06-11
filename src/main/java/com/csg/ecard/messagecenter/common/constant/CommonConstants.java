package com.csg.ecard.messagecenter.common.constant;

/**
 * 基础框架公共常量。
 * <p>
 * 放置跨框架层复用的通用 key，避免过滤器、上下文和审计填充逻辑重复硬编码。
 */
public final class CommonConstants {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String DEFAULT_OPERATOR = "system";

    private CommonConstants() {
    }
}
