package com.csg.ecard.messagecenter.infrastructure.organization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

/**
 * 内网组织接口返回对象，只保留当前业务需要字段。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class RemoteOrganizationDTO {

    private String orgId;

    private String orgName;

    private String orgCode;

    private String parentOrgId;

    private String nameFullPath;

    private Integer orgLevel;

    private Integer state;

    private Integer sortNo;

    private Timestamp updateTime;

    private List<RemoteOrganizationDTO> children;
}
