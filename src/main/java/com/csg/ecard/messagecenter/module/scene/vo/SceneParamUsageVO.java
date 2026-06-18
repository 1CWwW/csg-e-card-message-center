package com.csg.ecard.messagecenter.module.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 场景参数引用结果。
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "场景参数引用结果")
public class SceneParamUsageVO {

    @Schema(description = "是否被引用")
    private Boolean used;

    @Schema(description = "引用数量", type = "integer", format = "int64")
    private Long usageCount;

    @Schema(description = "引用模板")
    private List<SceneParamUsageTemplateVO> templates = Collections.emptyList();

    /**
     * 构造参数引用结果，并兼容已有调用中的模板名称字符串。
     *
     * @param used       是否引用
     * @param usageCount 引用模板数量
     * @param templates  模板信息或模板名称
     */
    public SceneParamUsageVO(Boolean used, Long usageCount, List<?> templates) {
        this.used = used;
        this.usageCount = usageCount;
        if (templates == null || templates.isEmpty()) {
            this.templates = Collections.emptyList();
            return;
        }
        this.templates = templates.stream()
                .map(item -> item instanceof SceneParamUsageTemplateVO template
                        ? template
                        : new SceneParamUsageTemplateVO(null, String.valueOf(item)))
                .toList();
    }

    public static SceneParamUsageVO unused() {
        return new SceneParamUsageVO(Boolean.FALSE, 0L, Collections.emptyList());
    }
}
