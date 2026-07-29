package com.csg.ecard.messagecenter.module.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.utils.MessageCenterValidator;
import com.csg.ecard.messagecenter.module.scene.dto.SceneCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.ScenePageQueryDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.enums.SceneModule;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.SceneParamCountResult;
import com.csg.ecard.messagecenter.module.scene.mapper.SceneOverviewRow;
import com.csg.ecard.messagecenter.module.scene.mapper.SceneTemplateCountResult;
import com.csg.ecard.messagecenter.module.scene.service.MsgSceneService;
import com.csg.ecard.messagecenter.module.scene.vo.MsgSceneVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneCodeCheckVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneDisableCheckVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 场景管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class MsgSceneServiceImpl implements MsgSceneService {

    private static final String SCENE_NOT_FOUND_MESSAGE = "场景不存在";
    private static final String SCENE_CODE_DUPLICATE_MESSAGE = "场景编码已存在";
    private static final String SORT_FIELD_CREATE_TIME = "createTime";
    private static final String SORT_ORDER_ASC = "asc";
    private static final String SORT_ORDER_DESC = "desc";
    private final MsgSceneMapper msgSceneMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;

    @Override
    public SceneOverviewVO overview() {
        SceneOverviewRow row = msgSceneMapper.selectOverview();
        SceneOverviewVO vo = new SceneOverviewVO();
        vo.setTotal(value(row == null ? null : row.getTotal()));
        vo.setActiveCount(value(row == null ? null : row.getActiveCount()));
        vo.setParamTotal(value(row == null ? null : row.getParamTotal()));
        vo.setTemplateTotal(value(row == null ? null : row.getTemplateTotal()));
        vo.setAssociatedSceneCount(value(row == null ? null : row.getAssociatedSceneCount()));
        return vo;
    }

    @Override
    public PageResult<MsgSceneVO> page(ScenePageQueryDTO query) {
        validatePageQuery(query);
        ScenePageQueryDTO normalizedQuery = query;
        validateOptionalModule(normalizedQuery.getModule());
        validateOptionalStatus(normalizedQuery.getStatus());

        Page<MsgScene> page = new Page<>(normalizedQuery.getPageNum(), normalizedQuery.getPageSize());
        LambdaQueryWrapper<MsgScene> wrapper = new LambdaQueryWrapper<MsgScene>()
                .eq(StringUtils.hasText(normalizedQuery.getSceneCode()), MsgScene::getSceneCode, normalizedQuery.getSceneCode())
                .like(StringUtils.hasText(normalizedQuery.getSceneName()), MsgScene::getSceneName, normalizedQuery.getSceneName())
                .eq(StringUtils.hasText(normalizedQuery.getModule()), MsgScene::getModule, normalizedQuery.getModule())
                .eq(normalizedQuery.getStatus() != null, MsgScene::getStatus, normalizedQuery.getStatus());
        applyPageSort(wrapper, normalizedQuery);

        Page<MsgScene> result = msgSceneMapper.selectPage(page, wrapper);
        Map<Long, Long> paramCountMap = countParamsBySceneIds(result.getRecords().stream()
                .map(MsgScene::getId)
                .toList());
        Map<Long, Long> templateCountMap = countTemplatesBySceneIds(result.getRecords().stream()
                .map(MsgScene::getId)
                .toList());
        List<MsgSceneVO> list = result.getRecords().stream()
                .map(scene -> toVO(scene,
                        paramCountMap.getOrDefault(scene.getId(), 0L),
                        templateCountMap.getOrDefault(scene.getId(), 0L)))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MsgSceneVO detail(Long id) {
        MsgScene scene = requireScene(id);
        return toVO(scene, countParamsBySceneId(scene.getId()), countTemplatesBySceneId(scene.getId()));
    }

    @Override
    public SceneDisableCheckVO disableCheck(Long id) {
        MsgScene scene = requireScene(id);
        return new SceneDisableCheckVO(countEnabledTemplatesBySceneId(scene.getId()));
    }

    @Override
    public SceneCodeCheckVO checkCode(String sceneCode, Long excludeId) {
        String normalizedSceneCode = normalizeSceneCode(sceneCode);
        MessageCenterValidator.requireValidSceneCode(normalizedSceneCode);
        return new SceneCodeCheckVO(normalizedSceneCode, !existsSceneCode(normalizedSceneCode, excludeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgSceneVO create(SceneCreateDTO request) {
        String sceneCode = normalizeSceneCode(request.getSceneCode());
        String sceneName = normalizeSceneName(request.getSceneName());
        String module = normalizeModule(request.getModule());
        String description = normalizeDescription(request.getDescription());
        MessageCenterValidator.requireValidSceneCode(sceneCode);
        validateSceneName(sceneName);
        validateModule(module);
        MessageCenterValidator.requireValidDescription(description);
        if (existsSceneCode(sceneCode, null)) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, SCENE_CODE_DUPLICATE_MESSAGE);
        }

        MsgScene scene = new MsgScene();
        scene.setSceneCode(sceneCode);
        scene.setSceneName(sceneName);
        scene.setModule(module);
        scene.setDescription(description);
        scene.setStatus(resolveCreateStatus(request.getStatus()));
        insertScene(scene);
        return toVO(scene, 0L, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgSceneVO update(Long id, SceneUpdateDTO request) {
        requireScene(id);
        String sceneCode = normalizeSceneCode(request.getSceneCode());
        String sceneName = normalizeSceneName(request.getSceneName());
        String module = normalizeModule(request.getModule());
        String description = normalizeDescription(request.getDescription());
        MessageCenterValidator.requireValidSceneCode(sceneCode);
        validateSceneName(sceneName);
        validateModule(module);
        validateStatus(request.getStatus());
        MessageCenterValidator.requireValidDescription(description);
        if (existsSceneCode(sceneCode, id)) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, SCENE_CODE_DUPLICATE_MESSAGE);
        }

        MsgScene scene = new MsgScene();
        scene.setId(id);
        scene.setSceneCode(sceneCode);
        scene.setSceneName(sceneName);
        scene.setModule(module);
        scene.setDescription(description);
        scene.setStatus(request.getStatus());
        updateScene(scene);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireScene(id);
        Long templateCount = countTemplatesBySceneId(id);
        if (templateCount > 0) {
            throw new BizException(ErrorCode.DELETE_NOT_ALLOWED,
                    "当前场景存在 " + templateCount + " 个模板，无法删除。");
        }
        msgSceneMapper.logicalDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgSceneVO toggle(Long id) {
        MsgScene existed = requireScene(id);
        Integer nextStatus = CommonStatus.ENABLE.getCode().equals(existed.getStatus())
                ? CommonStatus.DISABLE.getCode()
                : CommonStatus.ENABLE.getCode();

        MsgScene scene = new MsgScene();
        scene.setId(id);
        scene.setStatus(nextStatus);
        msgSceneMapper.updateById(scene);
        existed.setStatus(nextStatus);
        return toVO(existed,
                countParamsBySceneId(existed.getId()),
                countTemplatesBySceneId(existed.getId()));
    }

    private void insertScene(MsgScene scene) {
        try {
            msgSceneMapper.insert(scene);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, SCENE_CODE_DUPLICATE_MESSAGE);
        }
    }

    private void updateScene(MsgScene scene) {
        try {
            msgSceneMapper.updateById(scene);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, SCENE_CODE_DUPLICATE_MESSAGE);
        }
    }

    private MsgScene requireScene(Long id) {
        MsgScene scene = msgSceneMapper.selectById(id);
        if (scene == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, SCENE_NOT_FOUND_MESSAGE);
        }
        return scene;
    }

    private boolean existsSceneCode(String sceneCode, Long excludeId) {
        Long count = msgSceneMapper.selectCount(new LambdaQueryWrapper<MsgScene>()
                .eq(MsgScene::getSceneCode, sceneCode)
                .ne(excludeId != null, MsgScene::getId, excludeId));
        return count != null && count > 0;
    }

    private void validateModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "所属模块不能为空");
        }
        try {
            SceneModule.fromCode(module);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "所属模块不合法");
        }
    }

    private void validateOptionalModule(String module) {
        if (StringUtils.hasText(module)) {
            validateModule(module);
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "启用状态不能为空");
        }
        try {
            CommonStatus.fromCode(status);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "启用状态不合法");
        }
    }

    private void validateSceneName(String sceneName) {
        if (!StringUtils.hasText(sceneName) || sceneName.length() > MessageCenterConstants.SCENE_NAME_MAX_LENGTH) {
            throw new BizException(ErrorCode.PARAM_ERROR, "场景名称不能为空且长度不能超过50");
        }
    }

    private void validateOptionalStatus(Integer status) {
        if (status != null) {
            validateStatus(status);
        }
    }

    private String normalizeSceneCode(String sceneCode) {
        return sceneCode == null ? null : sceneCode.trim();
    }

    private String normalizeSceneName(String sceneName) {
        return sceneName == null ? null : sceneName.trim();
    }

    private String normalizeModule(String module) {
        return module == null ? null : module.trim();
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private void validatePageQuery(ScenePageQueryDTO query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageNum不能为空且必须从1开始");
        }
        if (query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize不能为空且不能超过100");
        }
        validatePageSort(query);
    }

    private void validatePageSort(ScenePageQueryDTO query) {
        boolean hasSortField = StringUtils.hasText(query.getSortField());
        boolean hasSortOrder = StringUtils.hasText(query.getSortOrder());
        if (!hasSortField && !hasSortOrder) {
            return;
        }
        if (!SORT_FIELD_CREATE_TIME.equals(query.getSortField())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "排序字段不合法");
        }
        if (!SORT_ORDER_ASC.equals(query.getSortOrder())
                && !SORT_ORDER_DESC.equals(query.getSortOrder())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "排序方向不合法");
        }
    }

    private void applyPageSort(LambdaQueryWrapper<MsgScene> wrapper, ScenePageQueryDTO query) {
        if (SORT_ORDER_ASC.equals(query.getSortOrder())) {
            wrapper.orderByAsc(MsgScene::getCreateTime)
                    .orderByAsc(MsgScene::getId);
            return;
        }
        wrapper.orderByDesc(MsgScene::getCreateTime)
                .orderByDesc(MsgScene::getId);
    }

    private Integer resolveCreateStatus(Integer status) {
        if (status == null) {
            return CommonStatus.ENABLE.getCode();
        }
        validateStatus(status);
        return status;
    }

    private MsgSceneVO toVO(MsgScene scene, Long paramCount, Long templateCount) {
        MsgSceneVO vo = new MsgSceneVO();
        vo.setId(scene.getId());
        vo.setSceneCode(scene.getSceneCode());
        vo.setSceneName(scene.getSceneName());
        vo.setModule(scene.getModule());
        vo.setModuleDesc(resolveModuleDesc(scene.getModule()));
        vo.setDescription(scene.getDescription());
        vo.setStatus(scene.getStatus());
        vo.setStatusDesc(resolveStatusDesc(scene.getStatus()));
        vo.setParamCount(paramCount == null ? 0L : paramCount);
        vo.setTemplateCount(templateCount == null ? 0L : templateCount);
        vo.setCreatedAt(scene.getCreateTime());
        vo.setUpdatedAt(scene.getUpdateTime());
        return vo;
    }

    private Long countParamsBySceneId(Long sceneId) {
        if (sceneId == null) {
            return 0L;
        }
        Long count = msgSceneParamMapper.selectCount(new LambdaQueryWrapper<MsgSceneParam>()
                .eq(MsgSceneParam::getSceneId, sceneId));
        return count == null ? 0L : count;
    }

    private Map<Long, Long> countParamsBySceneIds(List<Long> sceneIds) {
        if (sceneIds == null || sceneIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return msgSceneParamMapper.selectParamCountsBySceneIds(sceneIds).stream()
                .collect(Collectors.toMap(SceneParamCountResult::getSceneId,
                        SceneParamCountResult::getParamCount,
                        Long::sum));
    }

    private Long countTemplatesBySceneId(Long sceneId) {
        if (sceneId == null) {
            return 0L;
        }
        Long count = msgSceneMapper.selectTemplateCountBySceneId(sceneId);
        return count == null ? 0L : count;
    }

    private Long countEnabledTemplatesBySceneId(Long sceneId) {
        if (sceneId == null) {
            return 0L;
        }
        Long count = msgSceneMapper.selectEnabledTemplateCountBySceneId(sceneId);
        return count == null ? 0L : count;
    }

    private Map<Long, Long> countTemplatesBySceneIds(List<Long> sceneIds) {
        if (sceneIds == null || sceneIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SceneTemplateCountResult> results = msgSceneMapper.selectTemplateCountsBySceneIds(sceneIds);
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        return results.stream()
                .collect(Collectors.toMap(SceneTemplateCountResult::getSceneId,
                        SceneTemplateCountResult::getTemplateCount,
                        Long::sum));
    }

    private String resolveModuleDesc(String module) {
        return StringUtils.hasText(module) ? SceneModule.fromCode(module).getDesc() : null;
    }

    private String resolveStatusDesc(Integer status) {
        return status == null ? null : CommonStatus.fromCode(status).getDesc();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
