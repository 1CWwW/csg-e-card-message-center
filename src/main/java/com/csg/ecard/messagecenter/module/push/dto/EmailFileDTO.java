package com.csg.ecard.messagecenter.module.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 邮件附件信息。
 */
@Getter
@Setter
@Schema(description = "邮件附件信息")
public class EmailFileDTO {

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;
}
