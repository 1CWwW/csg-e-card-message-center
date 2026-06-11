package com.csg.ecard.messagecenter.framework.context;

/**
 * 当前用户上下文占位能力。
 * <p>
 * 通过 ThreadLocal 保存一次请求内的用户信息，供审计字段填充等基础能力读取。
 * 当前项目未实现认证系统，后续接入认证后可在过滤器或拦截器中写入真实用户信息。
 */
public final class CurrentUserContext {

    /**
     * 使用 ThreadLocal 隔离不同请求线程的用户信息。
     * 请求结束必须调用 {@link #clear()}，避免线程池复用时发生上下文残留。
     */
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
     * @return 用户信息；未设置时返回 null
     */
    public static UserInfo get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户 ID，未设置时返回默认值。
     *
     * @param defaultUserId 默认用户 ID
     * @return 当前用户 ID 或默认用户 ID
     */
    public static String getUserIdOrDefault(String defaultUserId) {
        UserInfo userInfo = HOLDER.get();
        return userInfo == null || userInfo.userId() == null ? defaultUserId : userInfo.userId();
    }

    /**
     * 清理当前线程上下文。
     * <p>
     * 必须在请求结束或任务执行完成后调用，防止 ThreadLocal 数据泄漏。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前用户信息占位模型。
     *
     * @param userId   用户 ID
     * @param userName 用户名称
     * @param tenantId 租户或组织 ID
     */
    public record UserInfo(String userId, String userName, String tenantId) {
    }
}
