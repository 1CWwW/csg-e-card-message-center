package com.csg.ecard.messagecenter.module.template.blockly;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * Blockly 内容校验结果。
 */
@Getter
@AllArgsConstructor
public class BlocklyValidationResult {

    private JsonNode blocklyJson;

    private boolean hasContent;

    private boolean valid;

    private List<String> errors;

    private Set<Long> referencedParamIds;
}
