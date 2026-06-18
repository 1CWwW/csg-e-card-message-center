package com.csg.ecard.messagecenter.module.scene.usage;

import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageTemplateVO;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 场景级参数引用索引。
 */
@Getter
public class SceneParamUsageIndex {

    private final Map<Long, List<SceneParamUsageTemplateVO>> references;

    public SceneParamUsageIndex(Map<Long, List<SceneParamUsageTemplateVO>> references) {
        this.references = references == null ? Collections.emptyMap() : Map.copyOf(references);
    }

    public List<SceneParamUsageTemplateVO> getTemplates(Long paramId) {
        return references.getOrDefault(paramId, Collections.emptyList());
    }
}
