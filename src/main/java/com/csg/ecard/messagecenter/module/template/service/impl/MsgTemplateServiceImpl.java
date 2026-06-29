package com.csg.ecard.messagecenter.module.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.DeleteFlag;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.enums.TemplateContentStatus;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamVO;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderResult;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationMode;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationResult;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCopyDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateContentSaveDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCreateDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePreviewDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateReferencePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplateUnit;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.mapper.TemplateQueryRow;
import com.csg.ecard.messagecenter.module.template.service.MsgTemplateService;
import com.csg.ecard.messagecenter.module.template.vo.TemplateCopyVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateContentVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplatePreviewVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateToolboxParamVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateToolboxVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息模板基础管理服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MsgTemplateServiceImpl implements MsgTemplateService {

    private static final String TEMPLATE_NOT_FOUND = "模板不存在";
    private static final String SCENE_NOT_FOUND = "场景不存在";
    private static final String SCENE_DISABLED = "只能选择启用场景";
    private static final String TEMPLATE_NAME_DUPLICATE = "当前场景下模板名称已存在";
    private static final String CONTENT_REQUIRED = "请先编辑并保存模板内容后再启用。";

    private final MsgTemplateMapper msgTemplateMapper;
    private final MsgTemplateUnitMapper msgTemplateUnitMapper;
    private final MsgSceneMapper msgSceneMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;
    private final BlocklyJsonValidator blocklyJsonValidator;
    private final BlocklyRenderer blocklyRenderer;

    @Override
    public PageResult<TemplateListVO> page(TemplatePageQueryDTO query) {
        validatePageQuery(query);
        validateOptionalChannelType(query.getChannelType());
        validateOptionalStatus(query.getStatus());
        validateContentStatus(query.getContentStatus());
        normalizeQuery(query);

        Page<TemplateQueryRow> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<TemplateQueryRow> result = msgTemplateMapper.selectTemplatePage(page, query);
        List<TemplateListVO> list = result.getRecords().stream()
                .map(this::toListVO)
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public TemplateDetailVO detail(Long id) {
        requireTemplate(id);
        TemplateQueryRow row = msgTemplateMapper.selectTemplateDetail(id);
        if (row == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, TEMPLATE_NOT_FOUND);
        }
        TemplateDetailVO vo = toDetailVO(row);
        List<String> unitIds = msgTemplateUnitMapper.selectUnitIdsByTemplateId(id);
        vo.setUnitIds(unitIds == null ? Collections.emptyList() : unitIds);
        vo.setBlocklyJson(blocklyJsonValidator.readNullable(row.getBlocklyJson()));
        vo.setSceneParams(listSceneParams(row.getSceneId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailVO create(TemplateCreateDTO request) {
        validateTemplateName(request.getTemplateName());
        MsgScene scene = requireScene(request.getSceneId(), true);
        ChannelType channelType = requireChannelType(request.getChannelType());
        ensureTemplateNameUnique(scene.getId(), request.getTemplateName(), null);
        List<String> unitIds = normalizeUnitIds(request.getUnitIds());

        MsgTemplate template = new MsgTemplate();
        template.setTemplateName(request.getTemplateName().trim());
        template.setSceneId(scene.getId());
        template.setChannelType(channelType.getCode());
        template.setBlocklyJson(null);
        template.setStatus(CommonStatus.DISABLE.getCode());
        msgTemplateMapper.insert(template);
        saveUnits(template.getId(), unitIds);
        return detail(template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailVO update(Long id, TemplateUpdateDTO request) {
        MsgTemplate existed = requireTemplate(id);
        validateTemplateName(request.getTemplateName());
        ChannelType channelType = requireChannelType(request.getChannelType());
        if (!Objects.equals(existed.getChannelType(), channelType.getCode())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "编辑模板时不允许修改渠道类型");
        }
        validateStatus(request.getStatus());
        ensureTemplateNameUnique(existed.getSceneId(), request.getTemplateName(), id);
        if (CommonStatus.DISABLE.getCode().equals(existed.getStatus())
                && CommonStatus.ENABLE.getCode().equals(request.getStatus())
                ) {
            validateForEnable(existed);
        }
        List<String> unitIds = normalizeUnitIds(request.getUnitIds());

        MsgTemplate update = new MsgTemplate();
        update.setId(id);
        update.setTemplateName(request.getTemplateName().trim());
        update.setChannelType(channelType.getCode());
        update.setStatus(request.getStatus());
        msgTemplateMapper.updateById(update);

        msgTemplateUnitMapper.deleteByTemplateId(id);
        saveUnits(id, unitIds);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTemplate(id);
        msgTemplateUnitMapper.deleteByTemplateId(id);
        msgTemplateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailVO toggle(Long id) {
        MsgTemplate existed = requireTemplate(id);
        Integer nextStatus;
        if (CommonStatus.ENABLE.getCode().equals(existed.getStatus())) {
            nextStatus = CommonStatus.DISABLE.getCode();
        } else {
            validateForEnable(existed);
            nextStatus = CommonStatus.ENABLE.getCode();
        }

        MsgTemplate update = new MsgTemplate();
        update.setId(id);
        update.setStatus(nextStatus);
        msgTemplateMapper.updateById(update);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateCopyVO copy(Long id, TemplateCopyDTO request) {
        MsgTemplate source = requireTemplate(id);
        validateTemplateName(request.getTemplateName());
        MsgScene targetScene = requireScene(request.getSceneId(), true);
        ensureTemplateNameUnique(targetScene.getId(), request.getTemplateName(), null);
        List<String> unitIds = normalizeUnitIds(request.getUnitIds());

        MsgTemplate copied = new MsgTemplate();
        copied.setTemplateName(request.getTemplateName().trim());
        copied.setSceneId(targetScene.getId());
        copied.setChannelType(source.getChannelType());
        if (Boolean.TRUE.equals(request.getCopyContent())) {
            copied.setBlocklyJson(source.getBlocklyJson());
        } else {
            copied.setBlocklyJson(null);
        }
        copied.setStatus(CommonStatus.DISABLE.getCode());
        msgTemplateMapper.insert(copied);
        saveUnits(copied.getId(), unitIds);
        return new TemplateCopyVO(copied.getId(), copied.getTemplateName(), hasContent(copied.getBlocklyJson()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateContentVO saveContent(Long id, TemplateContentSaveDTO request) {
        MsgTemplate template = requireTemplate(id);
        requireScene(template.getSceneId(), false);
        BlocklyValidationResult validation = blocklyJsonValidator.validateWorkspace(
                request.getSchemaVersion(),
                request.getWorkspace(),
                template.getSceneId(),
                loadSceneParamMap(template.getSceneId()),
                BlocklyValidationMode.DRAFT);
        String serialized = blocklyJsonValidator.write(validation.getBlocklyJson());
        boolean changed = contentChanged(template.getBlocklyJson(), validation.getBlocklyJson());

        MsgTemplate update = new MsgTemplate();
        update.setId(id);
        update.setBlocklyJson(serialized);
        if (changed && CommonStatus.ENABLE.getCode().equals(template.getStatus())) {
            update.setStatus(CommonStatus.DISABLE.getCode());
        }
        int updated = msgTemplateMapper.updateById(update);
        if (updated == 0) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "模板内容更新失败，请刷新后重试");
        }

        MsgTemplate saved = requireTemplate(id);
        JsonNode savedBlocklyJson = blocklyJsonValidator.readNullable(saved.getBlocklyJson());
        TemplateContentVO vo = new TemplateContentVO();
        vo.setTemplateId(saved.getId());
        vo.setBlocklyJson(savedBlocklyJson);
        vo.setHasContent(hasContent(saved.getBlocklyJson()));
        vo.setValid(validation.isValid());
        vo.setErrors(validation.getErrors());
        vo.setUpdatedAt(saved.getUpdateTime());
        return vo;
    }

    @Override
    public TemplatePreviewVO preview(TemplatePreviewDTO request) {
        if (request == null || !StringUtils.hasText(request.getTemplateId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "templateId不能为空");
        }

        MsgTemplate template = requireTemplate(parseTemplateId(request.getTemplateId()));
        Long sceneId = template.getSceneId();
        requireScene(sceneId, false);
        String channelType = requireChannelType(template.getChannelType()).getCode();
        Map<Long, MsgSceneParam> params = loadSceneParamMap(sceneId);

        BlocklyValidationResult validation;
        if (request.getWorkspace() != null) {
            Integer schemaVersion = request.getSchemaVersion() == null
                    ? BlocklyJsonValidator.SUPPORTED_SCHEMA_VERSION
                    : request.getSchemaVersion();
            validation = blocklyJsonValidator.validateWorkspace(
                    schemaVersion,
                    request.getWorkspace(),
                    sceneId,
                    params,
                    BlocklyValidationMode.DRAFT);
        } else {
            if (!hasContent(template.getBlocklyJson())) {
                throw new BizException(ErrorCode.TEMPLATE_EMPTY, "模板内容为空，无法预览");
            }
            validation = blocklyJsonValidator.validateStored(
                    template.getBlocklyJson(),
                    sceneId,
                    params,
                    BlocklyValidationMode.DRAFT);
        }

        BlocklyRenderResult rendered = blocklyRenderer.render(
                validation.getBlocklyJson(),
                sceneId,
                params,
                request.getValues());
        TemplatePreviewVO vo = new TemplatePreviewVO();
        vo.setTemplateId(template.getId());
        vo.setChannelType(channelType);
        vo.setRenderedContent(rendered.renderedContent());
        vo.setUsedParams(rendered.usedParams());
        vo.setWarnings(rendered.warnings());
        return vo;
    }

    @Override
    public TemplateToolboxVO toolbox(Long id) {
        MsgTemplate template = requireTemplate(id);
        TemplateToolboxVO vo = new TemplateToolboxVO();
        vo.setTemplateId(template.getId());
        vo.setSceneId(template.getSceneId());
        vo.setParams(listSceneParams(template.getSceneId()).stream()
                .map(this::toToolboxParamVO)
                .toList());
        return vo;
    }

    @Override
    public List<TemplateReferenceVO> references(Long id) {
        MsgTemplate current = requireTemplate(id);
        TemplateReferencePageQueryDTO query = new TemplateReferencePageQueryDTO();
        query.setSceneId(current.getSceneId());
        query.setChannelType(current.getChannelType());
        query.setContentStatus(1);
        query.setExcludeTemplateId(current.getId());
        return findReferenceItems(query).stream()
                .map(item -> toReferenceVO(item.row()))
                .toList();
    }

    @Override
    public PageResult<TemplateReferenceListVO> referencePage(TemplateReferencePageQueryDTO query) {
        validateReferenceQuery(query);
        query.setChannelType(trimToNull(query.getChannelType()));
        List<ReferenceItem> items = findReferenceItems(query);
        int fromIndex = Math.min((query.getPageNum() - 1) * query.getPageSize(), items.size());
        int toIndex = Math.min(fromIndex + query.getPageSize(), items.size());
        List<TemplateReferenceListVO> list = items.subList(fromIndex, toIndex).stream()
                .map(this::toReferenceListVO)
                .toList();
        return PageResult.of(list, items.size(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public TemplateReferenceDetailVO referenceDetail(Long id, Long referenceId) {
        MsgTemplate current = requireTemplate(id);
        MsgTemplate reference = requireTemplate(referenceId);
        if (Objects.equals(current.getId(), reference.getId())
                || !Objects.equals(current.getSceneId(), reference.getSceneId())
                || !Objects.equals(current.getChannelType(), reference.getChannelType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参考模板必须与当前模板属于同一场景和渠道类型");
        }
        BlocklyValidationResult validation = blocklyJsonValidator.validateStored(
                reference.getBlocklyJson(),
                reference.getSceneId(),
                loadSceneParamMap(reference.getSceneId()),
                BlocklyValidationMode.DRAFT);
        if (!validation.isHasContent() || !validation.isValid()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参考模板没有有效内容");
        }
        TemplateQueryRow row = msgTemplateMapper.selectTemplateDetail(referenceId);
        if (row == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, TEMPLATE_NOT_FOUND);
        }
        TemplateReferenceDetailVO vo = new TemplateReferenceDetailVO();
        fillReferenceVO(vo, row, validation.isHasContent());
        vo.setBlocklyJson(validation.getBlocklyJson());
        return vo;
    }

    private MsgTemplate requireTemplate(Long id) {
        MsgTemplate template = msgTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    private MsgScene requireScene(Long sceneId, boolean enabledRequired) {
        MsgScene scene = sceneId == null ? null : msgSceneMapper.selectById(sceneId);
        if (scene == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, SCENE_NOT_FOUND);
        }
        if (enabledRequired && !CommonStatus.ENABLE.getCode().equals(scene.getStatus())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, SCENE_DISABLED);
        }
        return scene;
    }

    private void ensureTemplateNameUnique(Long sceneId, String templateName, Long excludeId) {
        LambdaQueryWrapper<MsgTemplate> wrapper = new LambdaQueryWrapper<MsgTemplate>()
                .eq(MsgTemplate::getSceneId, sceneId)
                .eq(MsgTemplate::getTemplateName, templateName.trim())
                .eq(MsgTemplate::getDeleted, DeleteFlag.NORMAL.getCode())
                .ne(excludeId != null, MsgTemplate::getId, excludeId);
        Long count = msgTemplateMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, TEMPLATE_NAME_DUPLICATE);
        }
    }

    private void saveUnits(Long templateId, List<String> unitIds) {
        for (String unitId : unitIds) {
            MsgTemplateUnit unit = new MsgTemplateUnit();
            unit.setTemplateId(templateId);
            unit.setUnitId(unitId);
            msgTemplateUnitMapper.insert(unit);
        }
    }

    private List<String> normalizeUnitIds(List<String> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = unitIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(normalized);
    }

    private void validateTemplateName(String templateName) {
        if (!StringUtils.hasText(templateName) || templateName.trim().length() > 50) {
            throw new BizException(ErrorCode.PARAM_ERROR, "模板名称不能为空且长度不能超过50");
        }
    }

    private ChannelType requireChannelType(String channelType) {
        if (!StringUtils.hasText(channelType)) {
            throw new BizException(ErrorCode.CHANNEL_TYPE_INVALID, "渠道类型不能为空");
        }
        try {
            return ChannelType.fromCode(channelType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.CHANNEL_TYPE_INVALID, "渠道类型不合法");
        }
    }

    private void validateOptionalChannelType(String channelType) {
        if (StringUtils.hasText(channelType)) {
            requireChannelType(channelType);
        }
    }

    private void validateOptionalStatus(Integer status) {
        if (status == null) {
            return;
        }
        try {
            CommonStatus.fromCode(status);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "启停状态不合法");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "启停状态不能为空");
        }
        validateOptionalStatus(status);
    }

    private void validateContentStatus(Integer contentStatus) {
        if (contentStatus == null || contentStatus < 0 || contentStatus > 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "内容状态只能为0、1或2");
        }
    }

    private void validatePageQuery(TemplatePageQueryDTO query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageNum不能为空且必须从1开始");
        }
        if (query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize不能为空且不能超过100");
        }
    }

    private void normalizeQuery(TemplatePageQueryDTO query) {
        query.setTemplateName(trimToNull(query.getTemplateName()));
        query.setChannelType(trimToNull(query.getChannelType()));
        query.setUnitId(trimToNull(query.getUnitId()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasContent(String blocklyJson) {
        if (!StringUtils.hasText(blocklyJson)) {
            return false;
        }
        try {
            return !blocklyJsonValidator.isBlocklyContentEmpty(blocklyJson);
        } catch (BizException ex) {
            return true;
        }
    }

    private void validateForEnable(MsgTemplate template) {
        requireScene(template.getSceneId(), true);
        if (!hasContent(template.getBlocklyJson())) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, CONTENT_REQUIRED);
        }
        BlocklyValidationResult validation = blocklyJsonValidator.validateStored(
                template.getBlocklyJson(),
                template.getSceneId(),
                loadSceneParamMap(template.getSceneId()),
                BlocklyValidationMode.ENABLE);
        blocklyRenderer.validateRenderable(validation.getBlocklyJson());
    }

    private Long parseTemplateId(String templateId) {
        try {
            return Long.valueOf(templateId);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "templateId必须为有效ID字符串");
        }
    }

    private boolean contentChanged(String currentJson, JsonNode newJson) {
        if (!StringUtils.hasText(currentJson)) {
            return true;
        }
        try {
            return !Objects.equals(blocklyJsonValidator.readNullable(currentJson), newJson);
        } catch (BizException ex) {
            return true;
        }
    }

    private Map<Long, MsgSceneParam> loadSceneParamMap(Long sceneId) {
        List<MsgSceneParam> params = msgSceneParamMapper.selectList(
                new LambdaQueryWrapper<MsgSceneParam>()
                        .eq(MsgSceneParam::getSceneId, sceneId)
                        .orderByAsc(MsgSceneParam::getSortOrder)
                        .orderByAsc(MsgSceneParam::getId));
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        return params.stream().collect(Collectors.toMap(
                MsgSceneParam::getId,
                param -> param,
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private List<SceneParamVO> listSceneParams(Long sceneId) {
        List<MsgSceneParam> params = msgSceneParamMapper.selectList(
                new LambdaQueryWrapper<MsgSceneParam>()
                        .eq(MsgSceneParam::getSceneId, sceneId)
                        .orderByAsc(MsgSceneParam::getSortOrder)
                        .orderByAsc(MsgSceneParam::getId));
        if (params == null || params.isEmpty()) {
            return Collections.emptyList();
        }
        return params.stream().map(this::toSceneParamVO).toList();
    }

    private SceneParamVO toSceneParamVO(MsgSceneParam param) {
        SceneParamVO vo = new SceneParamVO();
        vo.setId(param.getId());
        vo.setSceneId(param.getSceneId());
        vo.setParamName(param.getParamName());
        vo.setParamLabel(param.getParamLabel());
        vo.setParamType(param.getParamType());
        vo.setParamTypeDesc(ParamType.fromCode(param.getParamType()).getDesc());
        vo.setSortOrder(param.getSortOrder());
        vo.setIsRequired(param.getIsRequired());
        vo.setUsageCount(0L);
        vo.setCreatedAt(param.getCreateTime());
        vo.setUpdatedAt(param.getUpdateTime());
        return vo;
    }

    private TemplateToolboxParamVO toToolboxParamVO(SceneParamVO param) {
        TemplateToolboxParamVO vo = new TemplateToolboxParamVO();
        vo.setParamId(param.getId());
        vo.setParamName(param.getParamName());
        vo.setParamLabel(param.getParamLabel());
        vo.setParamType(param.getParamType());
        vo.setParamTypeDesc(param.getParamTypeDesc());
        vo.setIsRequired(param.getIsRequired());
        vo.setSortOrder(param.getSortOrder());
        return vo;
    }

    private TemplateReferenceVO toReferenceVO(TemplateQueryRow row) {
        TemplateReferenceVO vo = new TemplateReferenceVO();
        fillReferenceVO(vo, row, true);
        return vo;
    }

    private void fillReferenceVO(TemplateReferenceVO vo,
                                 TemplateQueryRow row,
                                 boolean contentPresent) {
        vo.setTemplateId(row.getId());
        vo.setTemplateName(row.getTemplateName());
        vo.setSceneId(row.getSceneId());
        vo.setSceneName(row.getSceneName());
        vo.setChannelType(row.getChannelType());
        vo.setChannelTypeDesc(ChannelType.fromCode(row.getChannelType()).getDesc());
        vo.setStatus(row.getStatus());
        vo.setStatusDesc(CommonStatus.fromCode(row.getStatus()).getDesc());
        vo.setHasContent(contentPresent);
        vo.setUpdatedAt(row.getUpdateTime());
    }

    private TemplateReferenceListVO toReferenceListVO(ReferenceItem item) {
        TemplateQueryRow row = item.row();
        TemplateReferenceListVO vo = new TemplateReferenceListVO();
        vo.setId(row.getId());
        vo.setTemplateName(row.getTemplateName());
        vo.setSceneId(row.getSceneId());
        vo.setSceneName(row.getSceneName());
        vo.setChannelType(row.getChannelType());
        vo.setChannelTypeDesc(ChannelType.fromCode(row.getChannelType()).getDesc());
        vo.setHasContent(item.hasContent());
        vo.setUnitCount(row.getUnitCount() == null ? 0L : row.getUnitCount());
        vo.setStatus(row.getStatus());
        vo.setUpdatedAt(row.getUpdateTime());
        return vo;
    }

    private List<ReferenceItem> findReferenceItems(TemplateReferencePageQueryDTO query) {
        List<TemplateQueryRow> rows = msgTemplateMapper.selectReferenceCandidates(query);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Map<Long, MsgSceneParam>> paramsByScene = loadSceneParamMaps(
                rows.stream().map(TemplateQueryRow::getSceneId).distinct().toList());
        return rows.stream()
                .map(row -> new ReferenceItem(row,
                        validateReferenceContent(row,
                                paramsByScene.getOrDefault(row.getSceneId(), Collections.emptyMap()))))
                .filter(item -> matchesContentStatus(item.hasContent(), query.getContentStatus()))
                .toList();
    }

    private boolean validateReferenceContent(TemplateQueryRow row, Map<Long, MsgSceneParam> params) {
        if (!StringUtils.hasText(row.getBlocklyJson())) {
            return false;
        }
        try {
            BlocklyValidationResult validation = blocklyJsonValidator.validateStored(
                    row.getBlocklyJson(),
                    row.getSceneId(),
                    params,
                    BlocklyValidationMode.DRAFT);
            return validation.isHasContent() && validation.isValid();
        } catch (BizException ex) {
            log.warn("Reference template content is invalid. templateId={}, cause={}",
                    row.getId(), ex.getMessage());
            return false;
        }
    }

    private Map<Long, Map<Long, MsgSceneParam>> loadSceneParamMaps(List<Long> sceneIds) {
        if (sceneIds == null || sceneIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MsgSceneParam> params = msgSceneParamMapper.selectList(
                new LambdaQueryWrapper<MsgSceneParam>()
                        .in(MsgSceneParam::getSceneId, sceneIds));
        Map<Long, Map<Long, MsgSceneParam>> result = new LinkedHashMap<>();
        for (Long sceneId : sceneIds) {
            result.put(sceneId, new LinkedHashMap<>());
        }
        if (params != null) {
            for (MsgSceneParam param : params) {
                result.computeIfAbsent(param.getSceneId(), ignored -> new LinkedHashMap<>())
                        .put(param.getId(), param);
            }
        }
        return result;
    }

    private boolean matchesContentStatus(boolean hasContent, Integer contentStatus) {
        return contentStatus == null || contentStatus == 0
                || (contentStatus == 1 && hasContent)
                || (contentStatus == 2 && !hasContent);
    }

    private void validateReferenceQuery(TemplateReferencePageQueryDTO query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageNum不能为空且必须从1开始");
        }
        if (query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize不能为空且不能超过100");
        }
        validateContentStatus(query.getContentStatus());
        validateOptionalChannelType(query.getChannelType());
    }

    private record ReferenceItem(TemplateQueryRow row, boolean hasContent) {
    }

    private TemplateListVO toListVO(TemplateQueryRow row) {
        TemplateListVO vo = new TemplateListVO();
        fillListVO(vo, row);
        return vo;
    }

    private TemplateDetailVO toDetailVO(TemplateQueryRow row) {
        TemplateDetailVO vo = new TemplateDetailVO();
        fillListVO(vo, row);
        return vo;
    }

    private void fillListVO(TemplateListVO vo, TemplateQueryRow row) {
        boolean contentPresent = hasContent(row.getBlocklyJson());
        vo.setId(row.getId());
        vo.setTemplateName(row.getTemplateName());
        vo.setSceneId(row.getSceneId());
        vo.setSceneCode(row.getSceneCode());
        vo.setSceneName(row.getSceneName());
        vo.setChannelType(row.getChannelType());
        vo.setChannelTypeDesc(ChannelType.fromCode(row.getChannelType()).getDesc());
        vo.setUnitCount(row.getUnitCount() == null ? 0L : row.getUnitCount());
        vo.setHasContent(contentPresent);
        vo.setContentStatusDesc(contentPresent
                ? TemplateContentStatus.EDITED.getDesc()
                : TemplateContentStatus.EMPTY.getDesc());
        vo.setStatus(row.getStatus());
        vo.setStatusDesc(CommonStatus.fromCode(row.getStatus()).getDesc());
        vo.setCreatedAt(row.getCreateTime());
        vo.setUpdatedAt(row.getUpdateTime());
    }
}
