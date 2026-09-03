package com.csg.ecard.messagecenter.module.organization.service;

import com.csg.ecard.messagecenter.module.organization.dto.OrganizationResolveDTO;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationLazyNodeVO;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationNodeVO;

import java.util.List;

/**
 * 组织架构查询服务。
 */
public interface OrganizationService {

    /**
     * 查询组织树，用于前端单位下拉树。
     *
     * @return 组织树
     */
    List<OrganizationNodeVO> tree();

    /**
     * 查询根节点或指定组织的直接子节点。
     *
     * @param parentOrgId 父组织ID，为空时查询根节点
     * @return 单层组织节点
     */
    List<OrganizationLazyNodeVO> children(String parentOrgId);

    /**
     * 批量解析组织。
     *
     * @param request 批量组织ID
     * @return 扁平组织节点
     */
    List<OrganizationLazyNodeVO> resolve(OrganizationResolveDTO request);

    /**
     * 搜索当前用户可见的组织。
     *
     * @param keyword 名称或编码关键词
     * @return 搜索结果
     */
    List<OrganizationLazyNodeVO> search(String keyword);
}
