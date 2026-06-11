package com.csg.ecard.messagecenter.demo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Demo 请求体参数校验对象。
 * <p>
 * 仅用于验证 Jakarta Validation 与全局异常处理链路。
 */
@Getter
@Setter
@Schema(description = "Demo请求体验证对象")
public class DemoValidateRequest {

    @NotBlank(message = "name不能为空")
    @Schema(description = "名称", example = "message-center")
    private String name;

    @NotNull(message = "enabled不能为空")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
