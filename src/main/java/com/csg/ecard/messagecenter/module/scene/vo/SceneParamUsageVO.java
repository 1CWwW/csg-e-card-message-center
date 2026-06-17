package com.csg.ecard.messagecenter.module.scene.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@Schema(description = "场景参数引用结果")
public class SceneParamUsageVO {

    @Schema(description = "是否被引用")
    private Boolean used;

    @Schema(description = "引用数量", type = "integer", format = "int64")
    private Long usageCount;

    @Schema(description = "引用模板")
    private List<String> templates = Collections.emptyList();

    public static SceneParamUsageVO unused() {
        return new SceneParamUsageVO(Boolean.FALSE, 0L, Collections.emptyList());
    }
}
