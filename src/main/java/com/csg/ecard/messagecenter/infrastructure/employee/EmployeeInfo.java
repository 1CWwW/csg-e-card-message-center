package com.csg.ecard.messagecenter.infrastructure.employee;

/**
 * 员工中心用户基础信息。
 */
public record EmployeeInfo(String userId,
                           String userName,
                           String phone,
                           String email,
                           String orgId,
                           String orgName,
                           String elinkUserId) {
}
