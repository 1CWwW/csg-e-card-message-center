package com.csg.ecard.messagecenter.module.template.vo;

import com.csg.ecard.messagecenter.module.scene.vo.SceneParamVO;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 模板详情响应。
 */
@Getter
@Setter
@Schema(description = "模板详情响应")
public class TemplateDetailVO extends TemplateListVO {

    @Schema(description = "适用单位ID列表，空数组表示全量适用")
    private List<String> unitIds;

    @Schema(description = "Blockly内容JSON")
    private JsonNode blocklyJson;

    @Schema(description = "当前场景参数列表")
    private List<SceneParamVO> sceneParams;
}
