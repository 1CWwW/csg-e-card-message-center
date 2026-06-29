package com.csg.ecard.messagecenter.module.template.blockly;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Blockly JSON 集中解析、结构校验和静态类型校验器。
 */
@Component
@RequiredArgsConstructor
public class BlocklyJsonValidator {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final int MAX_CONTENT_BYTES = 1024 * 1024;
    private static final int MAX_BLOCK_COUNT = 2000;
    private static final int MAX_RECURSION_DEPTH = 100;
    private static final int MAX_ELSE_IF_COUNT = 10;
    private static final int MAX_SEPARATOR_LENGTH = 20;
    private static final String CONTENT_INPUT = "CONTENT";
    private static final Pattern CONDITIONAL_INPUT_PATTERN = Pattern.compile("^(IF|DO)(\\d+)$");

    private final ObjectMapper objectMapper;

    /**
     * 校验请求中的工作区并构造完整存储结构。
     */
    public BlocklyValidationResult validateWorkspace(Integer schemaVersion,
                                                      JsonNode workspace,
                                                      Long sceneId,
                                                      Map<Long, MsgSceneParam> params,
                                                      BlocklyValidationMode mode) {
        validateSchemaVersion(schemaVersion);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", schemaVersion);
        root.set("workspace", workspace == null ? objectMapper.nullNode() : workspace.deepCopy());
        write(root);
        return validateRoot(root, sceneId, params, mode);
    }

    /**
     * 解析并校验数据库中的完整 Blockly JSON。
     */
    public BlocklyValidationResult validateStored(String blocklyJson,
                                                   Long sceneId,
                                                   Map<Long, MsgSceneParam> params,
                                                   BlocklyValidationMode mode) {
        if (!StringUtils.hasText(blocklyJson)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板内容为空");
        }
        validateContentSize(blocklyJson);
        try {
            JsonNode root = objectMapper.readTree(blocklyJson);
            if (isBlocklyContentEmpty(root) && mode == BlocklyValidationMode.DRAFT) {
                return emptyResult(root);
            }
            return validateRoot(root, sceneId, params, mode);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板 Blockly JSON 无法解析");
        }
    }

    /**
     * 将校验后的 JSON 序列化为数据库字符串。
     */
    public String write(JsonNode blocklyJson) {
        try {
            String serialized = objectMapper.writeValueAsString(blocklyJson);
            validateContentSize(serialized);
            return serialized;
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "模板 Blockly JSON 序列化失败");
        }
    }

    /**
     * 将存储 JSON 读取为响应节点。
     */
    public JsonNode readNullable(String blocklyJson) {
        if (!StringUtils.hasText(blocklyJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(blocklyJson);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "模板 Blockly JSON 无法解析");
        }
    }

    /**
     * 按 Blockly Workspace 结构判断模板内容是否为空。
     */
    public boolean isBlocklyContentEmpty(String blocklyJson) {
        if (!StringUtils.hasText(blocklyJson)) {
            return true;
        }
        validateContentSize(blocklyJson);
        try {
            return isBlocklyContentEmpty(objectMapper.readTree(blocklyJson));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板 Blockly JSON 无法解析");
        }
    }

    /**
     * 按 Blockly Workspace 结构判断模板内容是否为空。
     */
    public boolean isBlocklyContentEmpty(JsonNode root) {
        if (root == null || root.isNull() || !root.isObject()) {
            return true;
        }
        JsonNode topBlocks = root.path("workspace").path("blocks").path("blocks");
        if (!topBlocks.isArray() || topBlocks.isEmpty()) {
            return true;
        }
        if (topBlocks.size() != 1
                || !BlocklyBlockTypes.MESSAGE_CONTENT.equals(text(topBlocks.get(0).get("type")))) {
            return false;
        }
        return optionalInputBlock(topBlocks.get(0), CONTENT_INPUT) == null;
    }

    /**
     * 递归提取 Workspace 中全部场景参数积木引用次数。
     */
    public Map<Long, Long> extractReferencedParamCounts(String blocklyJson) {
        if (!StringUtils.hasText(blocklyJson)) {
            return Collections.emptyMap();
        }
        validateContentSize(blocklyJson);
        try {
            Map<Long, Long> result = new LinkedHashMap<>();
            collectParamCounts(objectMapper.readTree(blocklyJson), result);
            return result;
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板 Blockly JSON 无法解析");
        }
    }

    private BlocklyValidationResult validateRoot(JsonNode root,
                                                  Long sceneId,
                                                  Map<Long, MsgSceneParam> params,
                                                  BlocklyValidationMode mode) {
        if (root == null || !root.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Blockly JSON 根节点必须为对象");
        }
        JsonNode versionNode = root.get("schemaVersion");
        if (versionNode == null || !versionNode.canConvertToInt()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "schemaVersion 不能为空且必须为整数");
        }
        validateSchemaVersion(versionNode.intValue());

        JsonNode workspace = root.get("workspace");
        if (workspace == null || !workspace.isObject()) {
            if (mode == BlocklyValidationMode.DRAFT) {
                return emptyResult(root);
            }
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace 不能为空且必须为对象");
        }
        JsonNode blocksContainer = workspace.get("blocks");
        if (blocksContainer == null || !blocksContainer.isObject()) {
            if (mode == BlocklyValidationMode.DRAFT) {
                return emptyResult(root);
            }
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace.blocks 不能为空且必须为对象");
        }
        JsonNode languageVersion = blocksContainer.get("languageVersion");
        if (languageVersion == null || !languageVersion.canConvertToInt()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "workspace.blocks.languageVersion 不能为空且必须为整数");
        }
        JsonNode topBlocks = blocksContainer.get("blocks");
        if (topBlocks == null || !topBlocks.isArray()) {
            if (mode == BlocklyValidationMode.DRAFT) {
                return emptyResult(root);
            }
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace.blocks.blocks 不能为空且必须为数组");
        }

        ValidationContext context = new ValidationContext(sceneId,
                params == null ? Collections.emptyMap() : params);
        boolean hasContent = validateTopBlocks(topBlocks, context);
        if (mode == BlocklyValidationMode.ENABLE && !hasContent) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "模板正文不能为空");
        }
        return new BlocklyValidationResult(root.deepCopy(),
                hasContent,
                hasContent,
                Collections.emptyList(),
                Set.copyOf(context.referencedParamIds),
                Map.copyOf(context.referencedParamCounts));
    }

    private boolean validateTopBlocks(JsonNode topBlocks, ValidationContext context) {
        if (topBlocks.isEmpty()) {
            return false;
        }
        List<JsonNode> contentRoots = topBlocks.findValues("type").isEmpty()
                ? Collections.emptyList()
                : findTopContentRoots(topBlocks);
        if (contentRoots.isEmpty()) {
            return false;
        }
        if (contentRoots.size() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "模板必须且只能存在一个 message_content 根节点");
        }
        JsonNode root = contentRoots.get(0);
        countNode(root, 1, context);
        JsonNode content = optionalInputBlock(root, CONTENT_INPUT);
        if (content == null) {
            return false;
        }
        BlocklyValueType type = validateNode(content, 2, context);
        requireTextOutput(type, content.path("type").asText());
        return true;
    }

    private List<JsonNode> findTopContentRoots(JsonNode topBlocks) {
        List<JsonNode> roots = new java.util.ArrayList<>();
        for (JsonNode topBlock : topBlocks) {
            if (BlocklyBlockTypes.MESSAGE_CONTENT.equals(text(topBlock.get("type")))) {
                roots.add(topBlock);
            }
        }
        return roots;
    }

    private BlocklyValidationResult emptyResult(JsonNode root) {
        return new BlocklyValidationResult(root == null ? objectMapper.nullNode() : root.deepCopy(),
                false,
                false,
                Collections.emptyList(),
                Collections.emptySet(),
                Collections.emptyMap());
    }

    private void collectParamCounts(JsonNode node, Map<Long, Long> result) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            String blockType = text(node.get("type"));
            if (BlocklyBlockTypes.isSceneParamType(blockType)) {
                JsonNode extraState = node.get("extraState");
                String paramIdText = extraState == null ? null : text(extraState.get("paramId"));
                if (StringUtils.hasText(paramIdText)) {
                    try {
                        result.merge(Long.valueOf(paramIdText), 1L, Long::sum);
                    } catch (NumberFormatException ex) {
                        throw new BizException(ErrorCode.PARAM_ERROR,
                                blockType + " 的 paramId 必须为有效 ID 字符串");
                    }
                }
            }
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectParamCounts(elements.next(), result);
            }
            return;
        }
        if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectParamCounts(elements.next(), result);
            }
        }
    }

    private BlocklyValueType validateNode(JsonNode block, int depth, ValidationContext context) {
        countNode(block, depth, context);
        String blockType = requireBlockType(block);
        if (!BlocklyBlockTypes.SUPPORTED_TYPES.contains(blockType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的 Blockly 节点类型：" + blockType);
        }
        BlocklyValueType type = switch (blockType) {
            case BlocklyBlockTypes.TEXT -> validateText(block);
            case BlocklyBlockTypes.TEXT_JOIN -> validateTextJoin(block, depth, context);
            case BlocklyBlockTypes.SCENE_PARAM_VALUE, BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF ->
                    validateParamReference(block, context);
            case BlocklyBlockTypes.AMOUNT_FORMAT -> validateFormatInput(
                    block, depth, context, BlocklyValueType.NUMBER, BlocklyValueType.STRING);
            case BlocklyBlockTypes.TIME_FORMAT -> validateFormatInput(
                    block, depth, context, BlocklyValueType.TIME, BlocklyValueType.STRING);
            case BlocklyBlockTypes.MATH_ARITHMETIC -> validateMathArithmetic(block, depth, context);
            case BlocklyBlockTypes.MATH_MODULO -> validateMathModulo(block, depth, context);
            case BlocklyBlockTypes.LOGIC_COMPARE -> validateLogicCompare(block, depth, context);
            case BlocklyBlockTypes.LOGIC_OPERATION -> validateLogicOperation(block, depth, context);
            case BlocklyBlockTypes.LOGIC_NEGATE -> validateLogicNegate(block, depth, context);
            case BlocklyBlockTypes.STRING_CONTAINS ->
                    validateStringBinary(block, depth, context, BlocklyBlockTypes.STRING_CONTAINS,
                            "TEXT", "SUBSTRING");
            case BlocklyBlockTypes.STRING_LIKE ->
                    validateStringBinary(block, depth, context, BlocklyBlockTypes.STRING_LIKE,
                            "TEXT", "PATTERN");
            case BlocklyBlockTypes.CONTROLS_IF -> validateControlsIf(block, depth, context);
            case BlocklyBlockTypes.CONTROLS_FOR_EACH -> validateControlsForEach(block, depth, context);
            case BlocklyBlockTypes.LOOP_ITEM_VALUE -> validateLoopItemValue(block, context);
            case BlocklyBlockTypes.MESSAGE_CONTENT ->
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "message_content 只能作为模板根节点");
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "不支持的 Blockly 节点类型：" + blockType);
        };
        validateAttachedNext(block, depth, context);
        return type;
    }

    private BlocklyValueType validateText(JsonNode block) {
        JsonNode value = block.path("fields").get("TEXT");
        if (value != null && !value.isTextual()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "text 的 TEXT 必须为字符串");
        }
        return BlocklyValueType.STRING;
    }

    private BlocklyValueType validateTextJoin(JsonNode block, int depth, ValidationContext context) {
        JsonNode inputs = block.get("inputs");
        if (inputs != null && !inputs.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "text_join 的 inputs 必须为对象");
        }
        if (inputs != null) {
            Iterator<Map.Entry<String, JsonNode>> iterator = inputs.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                JsonNode child = activeBlock(entry.getValue());
                if (child != null) {
                    BlocklyValueType type = validateNode(child, depth + 1, context);
                    if (type == BlocklyValueType.NUMBER && containsLoopItemValue(child)) {
                        throw new BizException(ErrorCode.PARAM_ERROR,
                                "NUMBER 类型循环元素输出正文时必须先经过格式化节点");
                    }
                    requireTextOutput(type, child.path("type").asText());
                }
            }
        }
        return BlocklyValueType.STRING;
    }

    private BlocklyValueType validateFormatInput(JsonNode block,
                                                  int depth,
                                                  ValidationContext context,
                                                  BlocklyValueType expected,
                                                  BlocklyValueType output) {
        String blockType = block.path("type").asText();
        JsonNode child = requireFirstInput(block, blockType);
        rejectAttachedNext(child, blockType);
        requireType(validateNode(child, depth + 1, context), expected,
                blockType + " 的输入类型必须为 " + expected);
        return output;
    }

    private BlocklyValueType validateMathArithmetic(JsonNode block, int depth, ValidationContext context) {
        requireOperator(block, Set.of("ADD", "MINUS", "MULTIPLY", "DIVIDE"));
        requireInputType(block, "A", BlocklyValueType.NUMBER, depth, context);
        requireInputType(block, "B", BlocklyValueType.NUMBER, depth, context);
        return BlocklyValueType.NUMBER;
    }

    private BlocklyValueType validateMathModulo(JsonNode block, int depth, ValidationContext context) {
        requireInputType(block, "DIVIDEND", BlocklyValueType.NUMBER, depth, context);
        requireInputType(block, "DIVISOR", BlocklyValueType.NUMBER, depth, context);
        return BlocklyValueType.NUMBER;
    }

    private BlocklyValueType validateLogicCompare(JsonNode block, int depth, ValidationContext context) {
        String operator = requireOperator(block, Set.of("EQ", "NEQ", "LT", "LTE", "GT", "GTE"));
        BlocklyValueType left = validateRequiredInput(block, "A", depth, context);
        BlocklyValueType right = validateRequiredInput(block, "B", depth, context);
        if ("EQ".equals(operator) || "NEQ".equals(operator)) {
            if (left != right || left == BlocklyValueType.STATEMENT) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "logic_compare 的 EQ、NEQ 只允许同类型值比较");
            }
        } else if (left != BlocklyValueType.NUMBER || right != BlocklyValueType.NUMBER) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "logic_compare 的大小比较输入必须为 NUMBER");
        }
        return BlocklyValueType.BOOLEAN;
    }

    private BlocklyValueType validateControlsIf(JsonNode block,
                                                 int depth,
                                                 ValidationContext context) {
        ControlsIfState state = controlsIfState(block);
        validateConditionalInputs(block, state);
        validateConditionInput(block, "IF0", depth, context);
        validateBranchInput(block, "DO0", depth, context);
        for (int index = 1; index <= state.elseIfCount(); index++) {
            validateConditionInput(block, "IF" + index, depth, context);
            validateBranchInput(block, "DO" + index, depth, context);
        }
        if (state.hasElse()) {
            validateBranchInput(block, "ELSE", depth, context);
        }
        return BlocklyValueType.STRING;
    }

    private ControlsIfState controlsIfState(JsonNode block) {
        JsonNode extraState = block.get("extraState");
        if (extraState == null || extraState.isNull()) {
            return new ControlsIfState(0, false);
        }
        if (!extraState.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 的 extraState 必须为对象");
        }
        JsonNode elseIfCountNode = extraState.get("elseIfCount");
        int elseIfCount = 0;
        if (elseIfCountNode != null) {
            if (!elseIfCountNode.isIntegralNumber() || !elseIfCountNode.canConvertToInt()
                    || elseIfCountNode.intValue() < 0) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "controls_if 的 elseIfCount 必须是大于等于 0 的整数");
            }
            elseIfCount = elseIfCountNode.intValue();
        }
        if (elseIfCount > MAX_ELSE_IF_COUNT) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 的 elseIfCount 不能超过 " + MAX_ELSE_IF_COUNT);
        }
        JsonNode hasElseNode = extraState.get("hasElse");
        boolean hasElse = false;
        if (hasElseNode != null) {
            if (!hasElseNode.isBoolean()) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "controls_if 的 hasElse 必须为 boolean");
            }
            hasElse = hasElseNode.booleanValue();
        }
        return new ControlsIfState(elseIfCount, hasElse);
    }

    private void validateConditionalInputs(JsonNode block, ControlsIfState state) {
        JsonNode inputs = block.get("inputs");
        if (inputs != null && !inputs.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 的 inputs 必须为对象");
        }
        if (inputs == null) {
            return;
        }
        Iterator<String> fieldNames = inputs.fieldNames();
        while (fieldNames.hasNext()) {
            String inputName = fieldNames.next();
            if ("ELSE".equals(inputName)) {
                if (!state.hasElse()) {
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "controls_if 声明了 ELSE 分支但 hasElse 为 false");
                }
                continue;
            }
            Matcher matcher = CONDITIONAL_INPUT_PATTERN.matcher(inputName);
            if (matcher.matches()) {
                int index;
                try {
                    index = Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException ex) {
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "controls_if 存在超出 elseIfCount 范围的输入 " + inputName);
                }
                if (index > state.elseIfCount()) {
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            "controls_if 存在超出 elseIfCount 范围的输入 " + inputName);
                }
            }
        }
    }

    private void validateConditionInput(JsonNode block,
                                        String inputName,
                                        int depth,
                                        ValidationContext context) {
        JsonNode child = optionalInputBlock(block, inputName);
        if (child == null) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 缺少条件输入 " + inputName);
        }
        rejectAttachedNext(child, "controls_if 的 " + inputName);
        BlocklyValueType type = validateNode(child, depth + 1, context);
        if (type != BlocklyValueType.BOOLEAN) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 的 " + inputName + " 必须返回布尔值");
        }
    }

    private void validateBranchInput(JsonNode block,
                                     String inputName,
                                     int depth,
                                     ValidationContext context) {
        JsonNode child = optionalInputBlock(block, inputName);
        if (child == null) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 缺少分支输入 " + inputName);
        }
        rejectAttachedNext(child, "controls_if 的 " + inputName);
        BlocklyValueType type = validateNode(child, depth + 1, context);
        if (type != BlocklyValueType.STRING && type != BlocklyValueType.TIME) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_if 的 " + inputName + " 必须返回字符串");
        }
    }

    private BlocklyValueType validateLogicOperation(JsonNode block, int depth, ValidationContext context) {
        requireOperator(block, Set.of("AND", "OR"));
        requireInputType(block, "A", BlocklyValueType.BOOLEAN, depth, context);
        requireInputType(block, "B", BlocklyValueType.BOOLEAN, depth, context);
        return BlocklyValueType.BOOLEAN;
    }

    private BlocklyValueType validateControlsForEach(JsonNode block, int depth, ValidationContext context) {
        if (context.loopItemType != null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 暂不支持嵌套循环");
        }

        JsonNode listBlock = optionalInputBlock(block, "LIST");
        if (listBlock == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 缺少输入 LIST");
        }
        rejectAttachedNext(listBlock, "controls_forEach 的 LIST");
        BlocklyValueType listType = validateRequiredInput(block, "LIST", depth, context);
        if (listType != BlocklyValueType.STRING_ARRAY && listType != BlocklyValueType.NUMBER_ARRAY) {
            throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 的 LIST 必须返回数组");
        }

        JsonNode bodyBlock = optionalInputBlock(block, "BODY");
        if (bodyBlock == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 缺少输入 BODY");
        }
        rejectAttachedNext(bodyBlock, "controls_forEach 的 BODY");

        JsonNode fields = block.get("fields");
        if (fields != null && !fields.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 的 fields 必须为对象");
        }
        JsonNode separatorField = fields == null ? null : fields.get("SEPARATOR");
        if (separatorField != null) {
            if (!separatorField.isTextual()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 的 SEPARATOR 必须是字符串");
            }
            if (separatorField.textValue().length() > MAX_SEPARATOR_LENGTH) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                    "controls_forEach 的 SEPARATOR 长度不能超过 " + MAX_SEPARATOR_LENGTH);
            }
        }

        BlocklyValueType itemType = listType == BlocklyValueType.STRING_ARRAY
            ? BlocklyValueType.STRING
            : BlocklyValueType.NUMBER;
        context.loopItemType = itemType;
        try {
            BlocklyValueType bodyType = validateRequiredInput(block, "BODY", depth, context);
            if (bodyType != BlocklyValueType.STRING) {
                throw new BizException(ErrorCode.PARAM_ERROR, "controls_forEach 的 BODY 必须返回字符串");
            }
        } finally {
            context.loopItemType = null;
        }

        return BlocklyValueType.STRING;
    }

    private BlocklyValueType validateLoopItemValue(JsonNode block, ValidationContext context) {
        if (context.loopItemType == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "loop_item_value 只能在循环体中使用");
        }

        JsonNode extraState = block.get("extraState");
        if (extraState == null || !extraState.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "loop_item_value 缺少 extraState");
        }

        JsonNode itemTypeNode = extraState.get("itemType");
        if (itemTypeNode == null || !itemTypeNode.isTextual()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "loop_item_value 缺少 itemType");
        }

        String itemTypeStr = itemTypeNode.asText();
        BlocklyValueType declaredType;
        try {
            declaredType = BlocklyValueType.valueOf(itemTypeStr);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                "loop_item_value 的 itemType 必须是 STRING 或 NUMBER");
        }

        if (declaredType != BlocklyValueType.STRING && declaredType != BlocklyValueType.NUMBER) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                "loop_item_value 的 itemType 必须是 STRING 或 NUMBER");
        }

        if (declaredType != context.loopItemType) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                "loop_item_value 的 itemType 与循环元素类型不一致");
        }

        return declaredType;
    }

    private boolean containsLoopItemValue(JsonNode block) {
        if (block == null || !block.isObject()) {
            return false;
        }
        if (BlocklyBlockTypes.LOOP_ITEM_VALUE.equals(block.path("type").asText())) {
            return true;
        }
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<JsonNode> iterator = inputs.elements();
            while (iterator.hasNext()) {
                if (containsLoopItemValue(activeBlock(iterator.next()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private BlocklyValueType validateLogicNegate(JsonNode block, int depth, ValidationContext context) {
        requireInputType(block, "BOOL", BlocklyValueType.BOOLEAN, depth, context);
        return BlocklyValueType.BOOLEAN;
    }

    private BlocklyValueType validateStringBinary(JsonNode block,
                                                   int depth,
                                                   ValidationContext context,
                                                   String blockType,
                                                   String leftInput,
                                                   String rightInput) {
        requireInputType(block, leftInput, BlocklyValueType.STRING, depth, context);
        requireInputType(block, rightInput, BlocklyValueType.STRING, depth, context);
        return BlocklyValueType.BOOLEAN;
    }

    private BlocklyValueType validateParamReference(JsonNode block, ValidationContext context) {
        String blockType = block.path("type").asText();
        JsonNode extraState = block.get("extraState");
        if (extraState == null || !extraState.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, blockType + " 缺少 extraState");
        }
        String sceneIdText = requiredText(extraState, "sceneId", blockType);
        String paramIdText = requiredText(extraState, "paramId", blockType);
        String paramName = requiredText(extraState, "paramName", blockType);
        String paramType = requiredText(extraState, "paramType", blockType);
        Long referenceSceneId = parseId(sceneIdText, "sceneId", blockType);
        Long paramId = parseId(paramIdText, "paramId", blockType);
        if (!referenceSceneId.equals(context.sceneId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, blockType + " 引用场景与模板场景不一致");
        }
        MsgSceneParam param = context.params.get(paramId);
        if (param == null || !context.sceneId.equals(param.getSceneId())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 引用参数不存在或不属于当前场景");
        }
        if (!param.getParamName().equals(paramName)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 参数名已变更，当前名称为 " + param.getParamName());
        }
        if (!param.getParamType().equals(paramType)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 参数类型已变更，当前类型为 " + param.getParamType());
        }
        context.referencedParamIds.add(paramId);
        context.referencedParamCounts.merge(paramId, 1L, Long::sum);
        try {
            return BlocklyValueType.fromParamType(paramType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 的 paramType 不支持：" + paramType);
        }
    }

    private void validateAttachedNext(JsonNode block, int depth, ValidationContext context) {
        JsonNode next = activeBlock(block.get("next"));
        if (next != null) {
            BlocklyValueType type = validateNode(next, depth + 1, context);
            requireTextOutput(type, next.path("type").asText());
        }
    }

    private void rejectAttachedNext(JsonNode block, String owner) {
        if (activeBlock(block.get("next")) != null) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    owner + " 不允许连接 next 节点");
        }
    }

    private BlocklyValueType validateRequiredInput(JsonNode block,
                                                   String inputName,
                                                   int depth,
                                                   ValidationContext context) {
        String blockType = block.path("type").asText();
        JsonNode child = optionalInputBlock(block, inputName);
        if (child == null) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 缺少输入 " + inputName);
        }
        rejectAttachedNext(child, blockType + " 的输入 " + inputName);
        return validateNode(child, depth + 1, context);
    }

    private void requireInputType(JsonNode block,
                                  String inputName,
                                  BlocklyValueType expected,
                                  int depth,
                                  ValidationContext context) {
        String blockType = block.path("type").asText();
        BlocklyValueType actual = validateRequiredInput(block, inputName, depth, context);
        requireType(actual, expected, blockType + " 的输入必须为 " + typeName(expected));
    }

    private String requireOperator(JsonNode block, Set<String> supported) {
        String blockType = block.path("type").asText();
        JsonNode operatorNode = block.path("fields").get("OP");
        String operator = text(operatorNode);
        if (!StringUtils.hasText(operator) || !supported.contains(operator)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 的操作符 OP 不支持");
        }
        return operator;
    }

    private JsonNode requireFirstInput(JsonNode block, String blockType) {
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            Iterator<JsonNode> iterator = inputs.elements();
            while (iterator.hasNext()) {
                JsonNode child = activeBlock(iterator.next());
                if (child != null) {
                    return child;
                }
            }
        }
        throw new BizException(ErrorCode.PARAM_ERROR, blockType + " 缺少输入");
    }

    private JsonNode optionalInputBlock(JsonNode block, String inputName) {
        JsonNode inputs = block.get("inputs");
        return inputs != null && inputs.isObject() ? activeBlock(inputs.get(inputName)) : null;
    }

    private JsonNode activeBlock(JsonNode wrapper) {
        if (wrapper == null || !wrapper.isObject()) {
            return null;
        }
        JsonNode block = wrapper.get("block");
        if (block != null && block.isObject()) {
            return block;
        }
        JsonNode shadow = wrapper.get("shadow");
        return shadow != null && shadow.isObject() ? shadow : null;
    }

    private void countNode(JsonNode block, int depth, ValidationContext context) {
        if (depth > MAX_RECURSION_DEPTH) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "Blockly 节点递归深度不能超过 " + MAX_RECURSION_DEPTH);
        }
        if (block == null || !block.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Blockly block 必须为对象");
        }
        context.blockCount++;
        if (context.blockCount > MAX_BLOCK_COUNT) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "Blockly 节点数量不能超过 " + MAX_BLOCK_COUNT);
        }
    }

    private String requireBlockType(JsonNode block) {
        String blockType = text(block.get("type"));
        if (!StringUtils.hasText(blockType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Blockly 节点缺少 type");
        }
        return blockType;
    }

    private void requireTextOutput(BlocklyValueType type, String blockType) {
        if (!type.canRenderAsText()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 的 BOOLEAN 结果不能直接输出为模板正文");
        }
    }

    private void requireType(BlocklyValueType actual, BlocklyValueType expected, String message) {
        if (actual != expected) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private String typeName(BlocklyValueType type) {
        return switch (type) {
            case STRING -> "字符串";
            case NUMBER -> "数字";
            case BOOLEAN -> "布尔值";
            case TIME -> "时间";
            case STRING_ARRAY -> "字符串数组";
            case NUMBER_ARRAY -> "数字数组";
            case STATEMENT -> "语句";
        };
    }

    private String requiredText(JsonNode node, String field, String blockType) {
        String value = text(node.get(field));
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 的 " + field + " 不能为空且必须为字符串");
        }
        return value;
    }

    private Long parseId(String value, String field, String blockType) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    blockType + " 的 " + field + " 必须为有效 ID 字符串");
        }
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private void validateSchemaVersion(Integer schemaVersion) {
        if (schemaVersion == null || schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "当前仅支持 schemaVersion=" + SUPPORTED_SCHEMA_VERSION);
        }
    }

    private void validateContentSize(String blocklyJson) {
        if (blocklyJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板内容不能超过 1MB");
        }
    }

    private static final class ValidationContext {

        private final Long sceneId;
        private final Map<Long, MsgSceneParam> params;
        private final Set<Long> referencedParamIds = new HashSet<>();
        private final Map<Long, Long> referencedParamCounts = new LinkedHashMap<>();
        private BlocklyValueType loopItemType;
        private int blockCount;

        private ValidationContext(Long sceneId, Map<Long, MsgSceneParam> params) {
            this.sceneId = sceneId;
            this.params = params;
        }
    }

    private record ControlsIfState(int elseIfCount, boolean hasElse) {
    }
}
