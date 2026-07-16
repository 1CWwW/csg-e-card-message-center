package com.csg.ecard.messagecenter.infrastructure.employee;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 公共 JADP 用户接口。
 */
@FeignClient(
        contextId = "jadpUserClient",
        name = "${public.jadp.service:public-jadp-service}",
        path = "${app.employee.remote.context-path:${public.jadp.service.contextPath:public-jadp-api}}",
        url = "${app.employee.remote.base-url:${public.jadp.service.url:}}"
)
public interface JadpUserClient {

    /**
     * 批量查询用户。
     *
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    @PostMapping(value = "/v1/user/getByIds", produces = "application/json;charset=UTF-8")
    List<RemoteUserDTO> getByUserIds(@RequestBody List<String> userIds);
}
