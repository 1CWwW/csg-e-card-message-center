package com.csg.ecard.messagecenter.module.push.service.impl;

import com.csg.ecard.messagecenter.infrastructure.employee.EmployeeInfoProvider;
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

    private final EmployeeInfoProvider employeeInfoProvider;

    @Override
    public List<String> resolve(String userOrgId) {
        return employeeInfoProvider.getUnitPath(userOrgId);
    }
}
