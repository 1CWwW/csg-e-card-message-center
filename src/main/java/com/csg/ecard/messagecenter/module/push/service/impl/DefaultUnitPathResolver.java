package com.csg.ecard.messagecenter.module.push.service.impl;

import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.push.service.UnitPathResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认单位路径解析器。
 */
@Component
@RequiredArgsConstructor
public class DefaultUnitPathResolver implements UnitPathResolver {

    private final OrganizationProvider organizationProvider;

    @Override
    public List<String> resolve(String userOrgId) {
        return organizationProvider.resolveUnitPath(userOrgId);
    }
}
