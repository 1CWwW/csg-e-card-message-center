package com.csg.ecard.messagecenter.module.scene.usage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageTemplateVO;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationMode;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于模板 Blockly 内容的场景参数引用检查器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultSceneParamUsageChecker implements SceneParamUsageChecker {

    private final MsgTemplateMapper msgTemplateMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;
    private final BlocklyJsonValidator blocklyJsonValidator;

    @Override
    public SceneParamUsageVO checkUsage(MsgSceneParam param) {
        SceneParamUsageIndex index = buildUsageIndex(param.getSceneId());
        List<SceneParamUsageTemplateVO> templates = index.getTemplates(param.getId());
        return new SceneParamUsageVO(
                !templates.isEmpty(),
                index.getUsageCount(param.getId()),
                templates);
    }

    @Override
    public SceneParamUsageIndex buildUsageIndex(Long sceneId) {
        List<MsgTemplate> templates = msgTemplateMapper.selectContentTemplatesBySceneId(sceneId);
        if (templates == null || templates.isEmpty()) {
            return new SceneParamUsageIndex(Collections.emptyMap());
        }
        Map<Long, MsgSceneParam> params = loadSceneParams(sceneId);
        Map<Long, Map<Long, SceneParamUsageTemplateVO>> mutableIndex = new LinkedHashMap<>();
        Map<Long, Long> usageCounts = new LinkedHashMap<>();

        for (MsgTemplate template : templates) {
            try {
                blocklyJsonValidator.validateStored(
                        template.getBlocklyJson(), sceneId, params, BlocklyValidationMode.DRAFT);
            } catch (BizException ex) {
                log.error("Template JSON cannot be parsed for usage check. templateId={}, cause={}",
                        template.getId(), ex.getMessage());
                throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                        "模板ID " + template.getId() + " 的内容无法解析，请先修复后再操作场景参数");
            }
            addReferences(mutableIndex, usageCounts, template,
                    blocklyJsonValidator.extractReferencedParamCounts(template.getBlocklyJson()));
        }

        Map<Long, List<SceneParamUsageTemplateVO>> result = mutableIndex.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue().values()),
                        (left, right) -> left,
                        LinkedHashMap::new));
        return new SceneParamUsageIndex(result, usageCounts);
    }

    private Map<Long, MsgSceneParam> loadSceneParams(Long sceneId) {
        List<MsgSceneParam> params = msgSceneParamMapper.selectList(
                new LambdaQueryWrapper<MsgSceneParam>()
                        .eq(MsgSceneParam::getSceneId, sceneId));
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        return params.stream().collect(Collectors.toMap(
                MsgSceneParam::getId,
                param -> param,
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private void addReferences(Map<Long, Map<Long, SceneParamUsageTemplateVO>> index,
                               Map<Long, Long> usageCounts,
                               MsgTemplate template,
                               Map<Long, Long> paramCounts) {
        SceneParamUsageTemplateVO reference = new SceneParamUsageTemplateVO(
                template.getId(),
                template.getTemplateName());
        for (Map.Entry<Long, Long> entry : paramCounts.entrySet()) {
            Long paramId = entry.getKey();
            index.computeIfAbsent(paramId, ignored -> new LinkedHashMap<>())
                    .putIfAbsent(template.getId(), reference);
            usageCounts.merge(paramId, entry.getValue(), Long::sum);
        }
    }
}
