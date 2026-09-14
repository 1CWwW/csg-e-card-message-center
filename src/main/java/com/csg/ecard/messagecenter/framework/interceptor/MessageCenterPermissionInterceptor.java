package com.csg.ecard.messagecenter.framework.interceptor;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.csg.ecard.messagecenter.infrastructure.security.JadpPermissionVerifier;
import com.csg.ecard.messagecenter.infrastructure.security.PermissionCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 消息中心后台接口菜单权限拦截器。
 * <p>
 * 根据请求路径匹配 JADP 菜单编码，并通过 JADP 权限接口校验当前登录用户是否可访问。
 */
@Component
public class MessageCenterPermissionInterceptor implements HandlerInterceptor {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String ORGANIZATION_PATH = "/api/msg/organization";
    private static final String DO_NOT_DISTURB_PATH = "/api/msg/do-not-disturb";
    private static final String RECORD_OVERVIEW_PATH = "/api/msg/record/overview";

    private static final String SCENE_CODE = "EcardMsgCenterScene";
    private static final String CHANNEL_CODE = "EcardMsgCenterChannel";
    private static final String TEMPLATE_CODE = "EcardMsgCenterTemplate";
    private static final String STATISTICS_CODE = "EcardMsgCenterStatistics";
    private static final String RECORD_CODE = "EcardMsgCenterRecord";

    private static final List<RoutePermission> ROUTE_PERMISSIONS = List.of(
            new RoutePermission("/api/msg/scene", List.of(SCENE_CODE)),
            new RoutePermission("/api/msg/channel", List.of(CHANNEL_CODE)),
            new RoutePermission("/api/msg/template", List.of(TEMPLATE_CODE)),
            new RoutePermission(DO_NOT_DISTURB_PATH, List.of(CHANNEL_CODE)),
            new RoutePermission("/api/msg/statistics", List.of(STATISTICS_CODE)),
            new RoutePermission(RECORD_OVERVIEW_PATH, List.of(RECORD_CODE, STATISTICS_CODE)),
            new RoutePermission("/api/msg/record", List.of(RECORD_CODE)),
            new RoutePermission(ORGANIZATION_PATH,
                    List.of(SCENE_CODE, CHANNEL_CODE, TEMPLATE_CODE, STATISTICS_CODE, RECORD_CODE))
    );

    private final JadpPermissionVerifier permissionVerifier;
    private final ObjectMapper objectMapper;

    public MessageCenterPermissionInterceptor(JadpPermissionVerifier permissionVerifier,
                                              ObjectMapper objectMapper) {
        this.permissionVerifier = permissionVerifier;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验消息中心后台请求的登录令牌和菜单权限。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否继续执行请求
     * @throws IOException 写入失败响应时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String requestPath = request.getServletPath();
        String accessToken = resolveAccessToken(request);
        if (accessToken == null) {
            writeFailure(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
            return false;
        }

        List<String> pageCodes = resolvePageCodes(requestPath);
        if (pageCodes.isEmpty()) {
            writeFailure(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
            return false;
        }

        for (String pageCode : pageCodes) {
            PermissionCheckResult result = permissionVerifier.verify(accessToken, pageCode);
            if (result == PermissionCheckResult.ALLOWED) {
                return true;
            }
            if (result == PermissionCheckResult.UNAUTHORIZED) {
                writeFailure(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                return false;
            }
            if (result == PermissionCheckResult.UNAVAILABLE) {
                writeFailure(response, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.EXTERNAL_SERVICE_ERROR);
                return false;
            }
        }

        writeFailure(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
        return false;
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String headerToken = request.getHeader(ACCESS_TOKEN);
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private List<String> resolvePageCodes(String requestPath) {
        return ROUTE_PERMISSIONS.stream()
                .filter(permission -> matchesPath(requestPath, permission.pathPrefix()))
                .findFirst()
                .map(RoutePermission::pageCodes)
                .orElseGet(List::of);
    }

    private boolean matchesPath(String requestPath, String pathPrefix) {
        return pathPrefix.equals(requestPath) || requestPath.startsWith(pathPrefix + "/");
    }

    private void writeFailure(HttpServletResponse response,
                              HttpStatus status,
                              ErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), CommonResult.fail(errorCode));
    }

    private record RoutePermission(String pathPrefix, List<String> pageCodes) {
    }
}
