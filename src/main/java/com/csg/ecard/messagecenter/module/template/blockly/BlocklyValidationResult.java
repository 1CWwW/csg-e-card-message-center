package com.csg.ecard.messagecenter.module.template.blockly;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blockly 内容校验结果。
 */
@Getter
public class BlocklyValidationResult {

    private JsonNode blocklyJson;

    private boolean hasContent;

    private boolean valid;

    private List<String> errors;

    private Set<Long> referencedParamIds;

    private Map<Long, Long> referencedParamCounts;

    public BlocklyValidationResult(JsonNode blocklyJson,
                                   boolean hasContent,
                                   boolean valid,
                                   List<String> errors,
                                   Set<Long> referencedParamIds,
                                   Map<Long, Long> referencedParamCounts) {
        this.blocklyJson = blocklyJson;
        this.hasContent = hasContent;
        this.valid = valid;
        this.errors = errors == null ? Collections.emptyList() : errors;
        this.referencedParamIds = referencedParamIds == null ? Collections.emptySet() : referencedParamIds;
        this.referencedParamCounts = referencedParamCounts == null
                ? Collections.emptyMap()
                : referencedParamCounts;
    }
}
