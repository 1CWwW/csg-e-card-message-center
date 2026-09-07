package com.csg.ecard.messagecenter.infrastructure.organization;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 公共 JADP 组织接口。
 */
@FeignClient(
        contextId = "jadpOrganizationClient",
        name = "${public.jadp.service:e-jadp-service}",
        path = "${public.jadp.service.contextPath:e-jadp}",
        url = "${public.jadp.service.url:}"
)
public interface JadpOrganizationClient {

    /**
     * 查询指定根单位下的单位及虚拟分组扁平列表。
     *
     * @param orgId 根单位ID
     * @return 包含根节点的组织数组，通过 parentOrgId 表达层级
     */
    @GetMapping(value = "/v1/organization/corpTree/{orgId}", produces = "application/json;charset=UTF-8")
    List<RemoteOrganizationDTO> queryCorpTree(@PathVariable("orgId") String orgId);

    /**
     * 查询全部组织，仅用于保留原有消息推送组织路径解析能力。
     *
     * @return 组织列表
     */
    @GetMapping(value = "/v1/organization/all/", produces = "application/json;charset=UTF-8")
    List<RemoteOrganizationDTO> queryAll();
}
