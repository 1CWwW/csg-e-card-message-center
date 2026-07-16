package com.csg.ecard.messagecenter.infrastructure.organization;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 公共 JADP 组织接口。
 */
@FeignClient(
        contextId = "jadpOrganizationClient",
        name = "${public.jadp.service:public-jadp-service}",
        path = "${app.organization.remote.context-path:${public.jadp.service.contextPath:public-jadp-api}}",
        url = "${app.organization.remote.base-url:${public.jadp.service.url:}}"
)
public interface JadpOrganizationClient {

    /**
     * 查询全部组织。
     *
     * @return 组织列表
     */
    @GetMapping(value = "/v1/organization/all/", produces = "application/json;charset=UTF-8")
    List<RemoteOrganizationDTO> queryAll();
}
