package com.csg.ecard.messagecenter.framework.filter;

import cn.hutool.core.util.IdUtil;
import com.csg.ecard.messagecenter.common.constant.CommonConstants;
import com.csg.ecard.messagecenter.framework.context.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求日志过滤器。
 * <p>
 * 为每个 HTTP 请求生成或透传 traceId，写入响应头和 MDC，并在请求结束后记录访问日志、清理上下文。
 */
@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    /**
     * 执行请求日志与上下文清理逻辑。
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = request.getHeader(CommonConstants.TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        MDC.put(CommonConstants.TRACE_ID, traceId);
        response.setHeader(CommonConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("HTTP {} {} status={} cost={}ms clientIp={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    cost,
                    request.getRemoteAddr());
            // 清理 ThreadLocal 和 MDC，避免容器线程复用时污染后续请求。
            CurrentUserContext.clear();
            MDC.remove(CommonConstants.TRACE_ID);
        }
    }
}
