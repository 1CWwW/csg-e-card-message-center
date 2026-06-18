package com.csg.ecard.messagecenter.module.template.blockly;

import java.util.Set;

/**
 * 模板 Blockly 节点类型定义。
 */
public final class BlocklyBlockTypes {

    public static final String MESSAGE_CONTENT = "message_content";
    public static final String TEXT = "text";
    public static final String TEXT_JOIN = "text_join";
    public static final String SCENE_PARAM_VALUE = "scene_param_value";

    /**
     * 旧版场景参数节点，仅用于兼容已有模板内容；新保存内容统一转换为 scene_param_value。
     */
    public static final String LEGACY_SCENE_PARAM_REF = "scene_param_ref";

    public static final String ARRAY_FOR_EACH = "array_for_each";
    public static final String CONTAINS = "contains";
    public static final String FUZZY_MATCH = "fuzzy_match";
    public static final String AMOUNT_FORMAT = "amount_format";
    public static final String TIME_FORMAT = "time_format";

    public static final Set<String> ENABLED_TYPES = Set.of(
            MESSAGE_CONTENT,
            SCENE_PARAM_VALUE,
            LEGACY_SCENE_PARAM_REF,
            TEXT, "text_multiline", TEXT_JOIN, "text_append", "text_length", "text_isEmpty",
            "text_indexOf", "text_charAt", "text_getSubstring", "text_changeCase", "text_trim",
            "math_number", "math_arithmetic", "math_single", "math_trig", "math_constant",
            "math_number_property", "math_round", "math_modulo", "math_constrain",
            "logic_compare", "logic_operation", "logic_negate", "logic_boolean", "logic_null",
            "logic_ternary", "controls_if", "lists_create_empty", "lists_create_with",
            "lists_repeat", "lists_length", "lists_isEmpty", "lists_indexOf", "lists_getIndex",
            "lists_setIndex", "lists_getSublist", "lists_split", "lists_sort",
            ARRAY_FOR_EACH, CONTAINS, FUZZY_MATCH, AMOUNT_FORMAT, TIME_FORMAT
    );

    private BlocklyBlockTypes() {
    }

    public static boolean isSceneParamType(String blockType) {
        return SCENE_PARAM_VALUE.equals(blockType) || LEGACY_SCENE_PARAM_REF.equals(blockType);
    }
}
