package com.csg.ecard.messagecenter.module.organization.service;

import java.util.Collection;

/**
 * 组织变更后的父节点缓存失效入口。
 */
public interface OrganizationCacheInvalidator {

    /**
     * 清理受影响父节点的直接子节点缓存。
     *
     * @param parentOrgIds 受影响父组织ID；null 表示根节点
     */
    void invalidateChildren(Collection<String> parentOrgIds);

    /**
     * 组织全量同步后清理全部直接子节点缓存。
     */
    void invalidateAllChildren();
}
