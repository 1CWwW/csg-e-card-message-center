package com.csg.ecard.messagecenter.framework.context;

/**
 * 当前用户上下文占位能力。
 * <p>
 * 当前阶段不实现登录认证，仅通过 ThreadLocal 保存一次请求内的用户和请求信息。
 * 请求结束后必须调用 {@link #clear()}，避免容器线程复用时发生上下文残留。
 */
public final class CurrentUserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    /**
     * 设置当前线程用户信息。
     *
     * @param userInfo 用户信息
     */
    public static void set(UserInfo userInfo) {
        HOLDER.set(userInfo);
    }

    /**
     * 获取当前线程用户信息。
     *
     * @return 用户信息，未设置时返回 null
     */
    public static UserInfo get() {
        return HOLDER.get();
    }

    public static String getUserId() {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null ? null : userInfo.userId();
    }

    public static String getUserName() {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null ? null : userInfo.userName();
    }

    public static String getOrgId() {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null ? null : userInfo.orgId();
    }

    public static String getOrgName() {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null ? null : userInfo.orgName();
    }

    public static String getRequestIp() {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null ? null : userInfo.requestIp();
    }

    public static String getRequestUri() {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null ? null : userInfo.requestUri();
    }

    /**
     * 获取当前用户 ID，未设置时返回默认值。
     *
     * @param defaultUserId 默认用户 ID
     * @return 当前用户 ID 或默认用户 ID
     */
    public static String getUserIdOrDefault(String defaultUserId) {
        String userId = getUserId();
        return userId == null ? defaultUserId : userId;
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前用户和请求信息占位模型。
     *
     * @param userId     用户 ID
     * @param userName   用户名称
     * @param orgId      所属组织 ID
     * @param orgName    所属组织名称
     * @param requestIp  请求 IP
     * @param requestUri 请求 URI
     */
    public static final class UserInfo {

        private final String userId;
        private final String userName;
        private final String orgId;
        private final String orgName;
        private final String requestIp;
        private final String requestUri;

        public UserInfo(String userId, String userName, String tenantId) {
            this(userId, userName, tenantId, null, null, null);
        }

        public UserInfo(String userId,
                        String userName,
                        String orgId,
                        String orgName,
                        String requestIp,
                        String requestUri) {
            this.userId = userId;
            this.userName = userName;
            this.orgId = orgId;
            this.orgName = orgName;
            this.requestIp = requestIp;
            this.requestUri = requestUri;
        }

        public String userId() {
            return userId;
        }

        public String userName() {
            return userName;
        }

        public String orgId() {
            return orgId;
        }

        public String orgName() {
            return orgName;
        }

        public String requestIp() {
            return requestIp;
        }

        public String requestUri() {
            return requestUri;
        }
    }
}
