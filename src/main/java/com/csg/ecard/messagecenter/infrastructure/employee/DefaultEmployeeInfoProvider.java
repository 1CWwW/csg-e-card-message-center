package com.csg.ecard.messagecenter.infrastructure.employee;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 默认员工信息提供实现，等待员工中心接入后替换。
 */
@Component
public class DefaultEmployeeInfoProvider implements EmployeeInfoProvider {

    @Override
    public Map<String, EmployeeInfo> listUsers(Collection<String> userIds) {
        return Map.of();
    }

    @Override
    public List<String> getUnitPath(String unitId) {
        return StringUtils.hasText(unitId) ? List.of(unitId) : List.of();
    }
}
