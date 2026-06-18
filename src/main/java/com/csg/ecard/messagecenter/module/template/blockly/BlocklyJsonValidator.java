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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;

/**
 * Blockly JSON 集中解析与基础校验器。
 */
@Component
@RequiredArgsConstructor
public class BlocklyJsonValidator {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final int MAX_CONTENT_BYTES = 1024 * 1024;
    private static final int MAX_BLOCK_COUNT = 2000;
    private static final int MAX_RECURSION_DEPTH = 100;

    private final ObjectMapper objectMapper;

    /**
     * 校验请求中的工作区并构造完整存储包装结构。
     *
     * @param schemaVersion schema版本
     * @param workspace     Blockly工作区
     * @param sceneId       模板场景ID
     * @param params        当前场景参数
     * @param mode          校验模式
     * @return 校验结果
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
     *
     * @param blocklyJson 存储JSON
     * @param sceneId     模板场景ID
     * @param params      当前场景参数
     * @param mode        校验模式
     * @return 校验结果
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
            return validateRoot(root, sceneId, params, mode);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板Blockly JSON无法解析");
        }
    }

    /**
     * 将校验后的 JSON 序列化为数据库字符串。
     *
     * @param blocklyJson Blockly JSON节点
     * @return JSON字符串
     */
    public String write(JsonNode blocklyJson) {
        try {
            String serialized = objectMapper.writeValueAsString(blocklyJson);
            validateContentSize(serialized);
            return serialized;
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "模板Blockly JSON序列化失败");
        }
    }

    /**
     * 将存储JSON读取为响应节点。
     *
     * @param blocklyJson 存储JSON
     * @return JSON节点，内容为空时返回null
     */
    public JsonNode readNullable(String blocklyJson) {
        if (!StringUtils.hasText(blocklyJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(blocklyJson);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "模板Blockly JSON无法解析");
        }
    }

    private BlocklyValidationResult validateRoot(JsonNode root,
                                                  Long sceneId,
                                                  Map<Long, MsgSceneParam> params,
                                                  BlocklyValidationMode mode) {
        if (root == null || !root.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Blockly JSON根节点必须为对象");
        }
        JsonNode versionNode = root.get("schemaVersion");
        if (versionNode == null || !versionNode.canConvertToInt()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "schemaVersion不能为空且必须为整数");
        }
        validateSchemaVersion(versionNode.intValue());

        JsonNode workspace = root.get("workspace");
        if (workspace == null || !workspace.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace不能为空且必须为对象");
        }
        JsonNode blocksContainer = workspace.get("blocks");
        if (blocksContainer == null || !blocksContainer.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace.blocks不能为空且必须为对象");
        }
        JsonNode languageVersion = blocksContainer.get("languageVersion");
        if (languageVersion == null || !languageVersion.canConvertToInt()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace.blocks.languageVersion不能为空且必须为整数");
        }
        JsonNode topBlocks = blocksContainer.get("blocks");
        if (topBlocks == null || !topBlocks.isArray()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "workspace.blocks.blocks不能为空且必须为数组");
        }

        ValidationContext context = new ValidationContext(sceneId,
                params == null ? Collections.emptyMap() : params,
                mode);
        for (JsonNode block : topBlocks) {
            traverseBlock(block, 1, context);
        }
        if (mode == BlocklyValidationMode.ENABLE && !context.unknownBlockTypes.isEmpty()) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                    "模板包含未知节点类型：" + String.join("、", context.unknownBlockTypes));
        }
        boolean hasContent = !topBlocks.isEmpty();
        if (mode == BlocklyValidationMode.ENABLE && !hasContent) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "模板工作区没有有效节点");
        }
        List<String> errors = context.unknownBlockTypes.stream()
                .map(type -> "未知节点类型：" + type)
                .toList();
        return new BlocklyValidationResult(root.deepCopy(),
                hasContent,
                hasContent && errors.isEmpty(),
                errors,
                Set.copyOf(context.referencedParamIds));
    }

    private void traverseBlock(JsonNode block, int depth, ValidationContext context) {
        if (depth > MAX_RECURSION_DEPTH) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "Blockly节点递归深度不能超过" + MAX_RECURSION_DEPTH);
        }
        if (block == null || !block.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Blockly block必须为对象");
        }
        context.blockCount++;
        if (context.blockCount > MAX_BLOCK_COUNT) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "Blockly节点数量不能超过" + MAX_BLOCK_COUNT);
        }

        String blockId = text(block.get("id"));
        String blockType = text(block.get("type"));
        if (!StringUtils.hasText(blockType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, locate(blockId, null) + "缺少type");
        }
        if (!BlocklyBlockTypes.ENABLED_TYPES.contains(blockType)) {
            context.unknownBlockTypes.add(blockType);
        }
        if (BlocklyBlockTypes.isSceneParamType(blockType)) {
            if (BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF.equals(blockType)
                    && block instanceof ObjectNode objectBlock) {
                objectBlock.put("type", BlocklyBlockTypes.SCENE_PARAM_VALUE);
            }
            validateParamReference(block, blockId, context);
        }

        JsonNode inputs = block.get("inputs");
        if (inputs != null) {
            if (!inputs.isObject()) {
                throw new BizException(ErrorCode.PARAM_ERROR, locate(blockId, null) + "inputs必须为对象");
            }
            Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode input = entry.getValue();
                if (input == null || !input.isObject()) {
                    throw new BizException(ErrorCode.PARAM_ERROR,
                            locate(blockId, null) + "input " + entry.getKey() + " 必须为对象");
                }
                if (input.has("block")) {
                    traverseBlock(input.get("block"), depth + 1, context);
                }
                if (input.has("shadow")) {
                    traverseBlock(input.get("shadow"), depth + 1, context);
                }
            }
        }

        JsonNode next = block.get("next");
        if (next != null) {
            if (!next.isObject()) {
                throw new BizException(ErrorCode.PARAM_ERROR, locate(blockId, null) + "next必须为对象");
            }
            if (next.has("block")) {
                traverseBlock(next.get("block"), depth + 1, context);
            }
        }
    }

    private void validateParamReference(JsonNode block, String blockId, ValidationContext context) {
        JsonNode extraState = block.get("extraState");
        if (extraState == null || !extraState.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, null) + "场景参数节点缺少extraState");
        }
        String sceneIdText = requiredText(extraState, "sceneId", blockId, null);
        String paramIdText = requiredText(extraState, "paramId", blockId, null);
        String paramName = requiredText(extraState, "paramName", blockId, null);
        String paramType = requiredText(extraState, "paramType", blockId, paramName);

        Long referenceSceneId = parseId(sceneIdText, "sceneId", blockId, paramName);
        Long paramId = parseId(paramIdText, "paramId", blockId, paramName);
        if (!referenceSceneId.equals(context.sceneId)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, paramName) + "引用场景与模板场景不一致");
        }
        MsgSceneParam param = context.params.get(paramId);
        if (param == null || !context.sceneId.equals(param.getSceneId())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, paramName) + "引用参数不存在或不属于当前场景");
        }
        if (!param.getParamName().equals(paramName)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, paramName) + "参数名已变更，当前名称为" + param.getParamName());
        }
        if (!param.getParamType().equals(paramType)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, paramName) + "参数类型已变更，当前类型为" + param.getParamType());
        }
        context.referencedParamIds.add(paramId);
    }

    private void validateSchemaVersion(Integer schemaVersion) {
        if (schemaVersion == null || schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "当前仅支持schemaVersion=" + SUPPORTED_SCHEMA_VERSION);
        }
    }

    private String requiredText(JsonNode node,
                                String field,
                                String blockId,
                                String paramName) {
        String value = text(node.get(field));
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, paramName) + field + "不能为空且必须为字符串");
        }
        return value;
    }

    private Long parseId(String value, String field, String blockId, String paramName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    locate(blockId, paramName) + field + "必须为有效ID字符串");
        }
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private String locate(String blockId, String paramName) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(blockId)) {
            parts.add("blockId=" + blockId);
        }
        if (StringUtils.hasText(paramName)) {
            parts.add("paramName=" + paramName);
        }
        return parts.isEmpty() ? "Blockly节点：" : "Blockly节点[" + String.join(", ", parts) + "]：";
    }

    private void validateContentSize(String blocklyJson) {
        if (blocklyJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板内容不能超过1MB。");
        }
    }

    private static final class ValidationContext {

        private final Long sceneId;
        private final Map<Long, MsgSceneParam> params;
        private final BlocklyValidationMode mode;
        private final Set<String> unknownBlockTypes = new LinkedHashSet<>();
        private final Set<Long> referencedParamIds = new HashSet<>();
        private int blockCount;

        private ValidationContext(Long sceneId,
                                  Map<Long, MsgSceneParam> params,
                                  BlocklyValidationMode mode) {
            this.sceneId = sceneId;
            this.params = params;
            this.mode = mode;
        }
    }
}
