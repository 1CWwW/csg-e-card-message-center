package com.csg.ecard.messagecenter.infrastructure.organization;

import java.util.Collection;
import java.util.List;

/**
 * 组织新增、修改、删除、启停或同步完成事件。
 *
 * @param affectedParentOrgIds 受影响父组织ID
 * @param fullRefresh          是否为全量同步
 */
public record OrganizationChangedEvent(Collection<String> affectedParentOrgIds, boolean fullRefresh) {

    public OrganizationChangedEvent {
        affectedParentOrgIds = affectedParentOrgIds == null ? List.of() : List.copyOf(affectedParentOrgIds);
    }

    /**
     * 创建全量组织同步完成事件。
     *
     * @return 全量同步事件
     */
    public static OrganizationChangedEvent fullSyncEvent() {
        return new OrganizationChangedEvent(List.of(), true);
    }
}
