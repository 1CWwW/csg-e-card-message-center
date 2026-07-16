package com.csg.ecard.messagecenter.infrastructure.organization;

import java.util.List;
import java.util.Collection;
import java.util.Set;

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
     * 查询指定父组织的直接子节点，父组织为空时查询根节点。
     *
     * @param parentOrgId 父组织ID
     * @return 直接子节点
     */
    List<OrganizationNode> children(String parentOrgId);

    /**
     * 批量判断哪些组织存在可展示的直接子节点。
     *
     * @param orgIds 组织ID集合
     * @return 存在直接子节点的组织ID
     */
    Set<String> parentOrgIdsWithChildren(Collection<String> orgIds);

    /**
     * 批量解析组织及其祖先路径。
     *
     * @param orgIds 组织ID集合
     * @return 组织路径，顺序与有效入参一致
     */
    List<OrganizationPath> resolve(Collection<String> orgIds);

    /**
     * 按组织名称或编码搜索，并返回组织路径。
     *
     * @param keyword    关键词
     * @param limit      最大返回数量
     * @param scopeOrgId 权限根组织ID，为空表示不限制
     * @return 匹配组织路径
     */
    List<OrganizationPath> search(String keyword, int limit, String scopeOrgId);

    /**
     * 判断组织是否位于指定权限根组织范围内。
     *
     * @param orgId      待判断组织ID
     * @param scopeOrgId 权限根组织ID
     * @return 是否在权限范围内
     */
    boolean isWithinScope(String orgId, String scopeOrgId);

    /**
     * 解析从当前单位到根单位的组织ID路径。
     *
     * @param orgId 当前组织ID
     * @return 当前单位到根单位的组织ID列表
     */
    List<String> resolveUnitPath(String orgId);
}
