package com.csg.ecard.messagecenter.infrastructure.security;

/**
 * JADP 菜单权限校验结果。
 */
public enum PermissionCheckResult {

    /** 拥有权限。 */
    ALLOWED,

    /** 已认证但没有权限。 */
    DENIED,

    /** 登录令牌缺失、无效或已过期。 */
    UNAUTHORIZED,

    /** JADP 权限服务不可用或返回异常结果。 */
    UNAVAILABLE
}
