package com.csg.ecard.messagecenter.infrastructure.employee;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 内网用户接口返回对象，只保留消息中心需要字段。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class RemoteUserDTO {

    private String userId;

    private String orgId;

    private String employeeId;

    private Integer state;

    private String account;

    private String employeeName;

    private String mobilePhone;

    private String email;

    private String elinkUserId;

    private Timestamp updateTime;
}
