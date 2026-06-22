package com.csg.ecard.messagecenter.module.template.blockly;

import java.util.Set;

/**
 * 模板 Blockly 节点类型统一定义。
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

    public static final String AMOUNT_FORMAT = "amount_format";
    public static final String TIME_FORMAT = "time_format";
    public static final String MATH_ARITHMETIC = "math_arithmetic";
    public static final String MATH_MODULO = "math_modulo";
    public static final String LOGIC_COMPARE = "logic_compare";
    public static final String LOGIC_OPERATION = "logic_operation";
    public static final String LOGIC_NEGATE = "logic_negate";
    public static final String STRING_CONTAINS = "string_contains";
    public static final String STRING_LIKE = "string_like";
    public static final String CONTROLS_IF = "controls_if";
    public static final String CONTROLS_FOR_EACH = "controls_forEach";
    public static final String LOOP_ITEM_VALUE = "loop_item_value";

    /**
     * 保存、预览和启用共同使用的受支持节点集合。
     */
    public static final Set<String> SUPPORTED_TYPES = Set.of(
            MESSAGE_CONTENT,
            TEXT,
            TEXT_JOIN,
            SCENE_PARAM_VALUE,
            LEGACY_SCENE_PARAM_REF,
            AMOUNT_FORMAT,
            TIME_FORMAT,
            MATH_ARITHMETIC,
            MATH_MODULO,
            LOGIC_COMPARE,
            LOGIC_OPERATION,
            LOGIC_NEGATE,
            STRING_CONTAINS,
            STRING_LIKE,
            CONTROLS_IF,
            CONTROLS_FOR_EACH,
            LOOP_ITEM_VALUE
    );

    private BlocklyBlockTypes() {
    }

    public static boolean isSceneParamType(String blockType) {
        return SCENE_PARAM_VALUE.equals(blockType) || LEGACY_SCENE_PARAM_REF.equals(blockType);
    }
}
