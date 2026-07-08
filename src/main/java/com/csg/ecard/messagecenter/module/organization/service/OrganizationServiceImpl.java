package com.csg.ecard.messagecenter.module.organization.service;

import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationNode;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 组织架构查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationProvider organizationProvider;

    @Override
    public List<OrganizationNodeVO> tree() {
        return organizationProvider.tree().stream()
                .map(this::toVO)
                .toList();
    }

    private OrganizationNodeVO toVO(OrganizationNode node) {
        OrganizationNodeVO vo = new OrganizationNodeVO();
        vo.setOrgId(node.getOrgId());
        vo.setOrgName(node.getOrgName());
        vo.setOrgCode(node.getOrgCode());
        vo.setParentOrgId(node.getParentOrgId());
        vo.setNameFullPath(node.getNameFullPath());
        vo.setOrgLevel(node.getOrgLevel());
        vo.setState(node.getState());
        vo.setChildren(node.getChildren() == null ? List.of() : node.getChildren().stream()
                .map(this::toVO)
                .toList());
        return vo;
    }
}
