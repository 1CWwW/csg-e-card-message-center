package com.csg.ecard.messagecenter.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 操作日志公共 DTO。
 * <p>
 * 仅用于后续业务模块传递操作日志数据，不在当前阶段创建日志表或接口。
 */
@Getter
@Setter
@Schema(description = "操作日志DTO")
public class OperationLogDTO {

    private String moduleName;
    private String operationType;
    private String bizId;
    private String bizName;
    private String beforeData;
    private String afterData;
    private String operatorId;
    private String operatorName;
    private String operatorOrgId;
    private String requestIp;
    private String requestUri;
    private LocalDateTime operationTime;
    private String result;
    private String errorMsg;
}
