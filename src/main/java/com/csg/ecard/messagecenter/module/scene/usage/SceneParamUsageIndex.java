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
    private final Map<Long, Long> usageCounts;

    public SceneParamUsageIndex(Map<Long, List<SceneParamUsageTemplateVO>> references) {
        this(references, null);
    }

    public SceneParamUsageIndex(Map<Long, List<SceneParamUsageTemplateVO>> references,
                                Map<Long, Long> usageCounts) {
        this.references = references == null ? Collections.emptyMap() : Map.copyOf(references);
        this.usageCounts = usageCounts == null ? Collections.emptyMap() : Map.copyOf(usageCounts);
    }

    public List<SceneParamUsageTemplateVO> getTemplates(Long paramId) {
        return references.getOrDefault(paramId, Collections.emptyList());
    }

    public long getUsageCount(Long paramId) {
        return usageCounts.getOrDefault(paramId, 0L);
    }
}
