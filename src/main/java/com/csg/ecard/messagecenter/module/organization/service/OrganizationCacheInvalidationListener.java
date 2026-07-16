package com.csg.ecard.messagecenter.module.organization.service;

import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 将组织变更事件转换为父节点缓存失效操作。
 */
@Component
@RequiredArgsConstructor
public class OrganizationCacheInvalidationListener {

    private final OrganizationCacheInvalidator cacheInvalidator;

    /**
     * 处理组织变更或同步完成事件。
     *
     * @param event 组织变更事件
     */
    @EventListener
    public void onOrganizationChanged(OrganizationChangedEvent event) {
        if (event.fullRefresh()) {
            cacheInvalidator.invalidateAllChildren();
        } else {
            cacheInvalidator.invalidateChildren(event.affectedParentOrgIds());
        }
    }
}
