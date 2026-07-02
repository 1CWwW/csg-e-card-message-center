package com.csg.ecard.messagecenter.infrastructure.employee;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 员工与组织信息提供接口。
 */
public interface EmployeeInfoProvider {

    /**
     * 批量获取用户基础信息。
     *
     * @param userIds 用户ID集合
     * @return 用户信息映射
     */
    Map<String, EmployeeInfo> listUsers(Collection<String> userIds);

    /**
     * 获取单位路径，按当前单位到上级单位排序。
     *
     * @param unitId 单位ID
     * @return 单位路径
     */
    List<String> getUnitPath(String unitId);
}
