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
import com.csg.ecard.messagecenter.module.scene.service.MsgSceneService;
import com.csg.ecard.messagecenter.module.scene.vo.MsgSceneVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneCodeCheckVO;
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
    private static final long UNAVAILABLE_RELATION_COUNT = 0L;

    private final MsgSceneMapper msgSceneMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;

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
                .eq(normalizedQuery.getStatus() != null, MsgScene::getStatus, normalizedQuery.getStatus())
                .orderByDesc(MsgScene::getCreateTime);

        Page<MsgScene> result = msgSceneMapper.selectPage(page, wrapper);
        Map<Long, Long> paramCountMap = countParamsBySceneIds(result.getRecords().stream()
                .map(MsgScene::getId)
                .toList());
        List<MsgSceneVO> list = result.getRecords().stream()
                .map(scene -> toVO(scene, paramCountMap.getOrDefault(scene.getId(), 0L)))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MsgSceneVO detail(Long id) {
        MsgScene scene = requireScene(id);
        return toVO(scene, countParamsBySceneId(scene.getId()));
    }

    @Override
    public SceneCodeCheckVO checkCode(String sceneCode) {
        MessageCenterValidator.requireValidSceneCode(sceneCode);
        return new SceneCodeCheckVO(sceneCode, !existsSceneCode(sceneCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgSceneVO create(SceneCreateDTO request) {
        MessageCenterValidator.requireValidSceneCode(request.getSceneCode());
        validateSceneName(request.getSceneName());
        validateModule(request.getModule());
        MessageCenterValidator.requireValidDescription(request.getDescription());
        if (existsSceneCode(request.getSceneCode())) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, SCENE_CODE_DUPLICATE_MESSAGE);
        }

        MsgScene scene = new MsgScene();
        scene.setSceneCode(request.getSceneCode());
        scene.setSceneName(request.getSceneName());
        scene.setModule(request.getModule());
        scene.setDescription(request.getDescription());
        scene.setStatus(resolveCreateStatus(request.getStatus()));
        insertScene(scene);
        return toVO(scene, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgSceneVO update(Long id, SceneUpdateDTO request) {
        requireScene(id);
        validateSceneName(request.getSceneName());
        validateModule(request.getModule());
        validateStatus(request.getStatus());
        MessageCenterValidator.requireValidDescription(request.getDescription());

        MsgScene scene = new MsgScene();
        scene.setId(id);
        scene.setSceneName(request.getSceneName());
        scene.setModule(request.getModule());
        scene.setDescription(request.getDescription());
        scene.setStatus(request.getStatus());
        msgSceneMapper.updateById(scene);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireScene(id);
        msgSceneMapper.deleteById(id);
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
        return toVO(existed, countParamsBySceneId(existed.getId()));
    }

    private void insertScene(MsgScene scene) {
        try {
            msgSceneMapper.insert(scene);
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

    private boolean existsSceneCode(String sceneCode) {
        Long count = msgSceneMapper.selectCount(new LambdaQueryWrapper<MsgScene>()
                .eq(MsgScene::getSceneCode, sceneCode));
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

    private void validatePageQuery(ScenePageQueryDTO query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageNum不能为空且必须从1开始");
        }
        if (query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > 100) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize不能为空且不能超过100");
        }
    }

    private Integer resolveCreateStatus(Integer status) {
        if (status == null) {
            return CommonStatus.ENABLE.getCode();
        }
        validateStatus(status);
        return status;
    }

    private MsgSceneVO toVO(MsgScene scene, Long paramCount) {
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
        vo.setTemplateCount(resolveTemplateCount(scene.getId()));
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

    private Long resolveTemplateCount(Long sceneId) {
        // 模板模块尚未实现，当前按需求返回 0；后续模块完成后在此替换为批量真实统计。
        return UNAVAILABLE_RELATION_COUNT;
    }

    private String resolveModuleDesc(String module) {
        return StringUtils.hasText(module) ? SceneModule.fromCode(module).getDesc() : null;
    }

    private String resolveStatusDesc(Integer status) {
        return status == null ? null : CommonStatus.fromCode(status).getDesc();
    }
}
