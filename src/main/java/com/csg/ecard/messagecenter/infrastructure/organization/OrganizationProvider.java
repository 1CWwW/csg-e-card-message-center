package com.csg.ecard.messagecenter.infrastructure.organization;

import java.util.List;

/**
 * 组织架构数据提供接口。
 */
public interface OrganizationProvider {

    /**
     * 查询完整组织树。
     *
     * @return 组织树
     */
    List<OrganizationNode> tree();

    /**
     * 解析从当前单位到根单位的组织ID路径。
     *
     * @param orgId 当前组织ID
     * @return 当前单位到根单位的组织ID列表
     */
    List<String> resolveUnitPath(String orgId);
}
