package com.csg.ecard.messagecenter.module.organization.service;

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
}
