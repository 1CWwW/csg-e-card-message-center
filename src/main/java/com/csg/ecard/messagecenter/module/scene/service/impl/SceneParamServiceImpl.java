package com.csg.ecard.messagecenter.module.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.utils.MessageCenterValidator;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamSortDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamSortItemDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.scene.service.SceneParamService;
import com.csg.ecard.messagecenter.module.scene.usage.SceneParamUsageChecker;
import com.csg.ecard.messagecenter.module.scene.usage.SceneParamUsageIndex;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 场景参数管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class SceneParamServiceImpl implements SceneParamService {

    private static final String SCENE_NOT_FOUND_MESSAGE = "场景不存在";
    private static final String PARAM_NOT_FOUND_MESSAGE = "场景参数不存在";
    private static final String PARAM_DUPLICATE_MESSAGE = "同一场景下参数名已存在";
    private static final int NOT_REQUIRED = 0;
    private static final int REQUIRED = 1;

    private final MsgSceneMapper msgSceneMapper;
    private final MsgSceneParamMapper msgSceneParamMapper;
    private final SceneParamUsageChecker sceneParamUsageChecker;

    @Override
    public List<SceneParamVO> list(Long sceneId) {
        requireScene(sceneId);
        List<MsgSceneParam> params = msgSceneParamMapper.selectList(new LambdaQueryWrapper<MsgSceneParam>()
                .eq(MsgSceneParam::getSceneId, sceneId)
                .orderByAsc(MsgSceneParam::getSortOrder));
        SceneParamUsageIndex usageIndex = sceneParamUsageChecker.buildUsageIndex(sceneId);
        return params.stream().map(param -> toVO(param, usageIndex)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SceneParamVO create(Long sceneId, SceneParamCreateDTO request) {
        requireScene(sceneId);
        validateParamName(request.getParamName());
        validateParamLabel(request.getParamLabel());
        validateParamType(request.getParamType());
        Integer isRequired = resolveIsRequired(request.getIsRequired());
        Integer sortOrder = resolveCreateSortOrder(sceneId, request.getSortOrder());
        ensureParamNameUnique(sceneId, request.getParamName(), null);

        MsgSceneParam param = new MsgSceneParam();
        param.setSceneId(sceneId);
        param.setParamName(request.getParamName());
        param.setParamLabel(request.getParamLabel());
        param.setParamType(request.getParamType());
        param.setSortOrder(sortOrder);
        param.setIsRequired(isRequired);
        insertParam(param);
        return toVO(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SceneParamVO update(Long sceneId, Long paramId, SceneParamUpdateDTO request) {
        requireScene(sceneId);
        MsgSceneParam existed = requireParam(sceneId, paramId);
        validateParamName(request.getParamName());
        validateParamLabel(request.getParamLabel());
        validateParamType(request.getParamType());
        Integer isRequired = resolveIsRequired(request.getIsRequired());
        Integer sortOrder = resolveUpdateSortOrder(request.getSortOrder(), existed.getSortOrder());
        ensureUsageAllowsUpdate(existed, request);
        ensureParamNameUnique(sceneId, request.getParamName(), paramId);

        MsgSceneParam param = new MsgSceneParam();
        param.setId(paramId);
        param.setParamName(request.getParamName());
        param.setParamLabel(request.getParamLabel());
        param.setParamType(request.getParamType());
        param.setSortOrder(sortOrder);
        param.setIsRequired(isRequired);
        updateParam(param);
        return toVO(requireParam(sceneId, paramId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long sceneId, Long paramId) {
        requireScene(sceneId);
        MsgSceneParam param = requireParam(sceneId, paramId);
        SceneParamUsageVO usage = sceneParamUsageChecker.checkUsage(param);
        if (Boolean.TRUE.equals(usage.getUsed())) {
            throw new BizException(ErrorCode.DELETE_NOT_ALLOWED, "参数已被引用，不能删除");
        }
        msgSceneParamMapper.logicalDeleteById(paramId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sort(Long sceneId, SceneParamSortDTO request) {
        requireScene(sceneId);
        validateSortRequest(request);
        List<Long> paramIds = request.getItems().stream().map(SceneParamSortItemDTO::getParamId).toList();
        List<MsgSceneParam> params = msgSceneParamMapper.selectList(new LambdaQueryWrapper<MsgSceneParam>()
                .eq(MsgSceneParam::getSceneId, sceneId)
                .in(MsgSceneParam::getId, paramIds));
        if (params.size() != paramIds.size()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "排序参数必须全部属于当前场景");
        }

        for (SceneParamSortItemDTO item : request.getItems()) {
            MsgSceneParam param = new MsgSceneParam();
            param.setId(item.getParamId());
            param.setSortOrder(item.getSortOrder());
            msgSceneParamMapper.updateById(param);
        }
    }

    @Override
    public SceneParamUsageVO usage(Long sceneId, Long paramId) {
        requireScene(sceneId);
        MsgSceneParam param = requireParam(sceneId, paramId);
        return normalizeUsage(sceneParamUsageChecker.checkUsage(param));
    }

    private void insertParam(MsgSceneParam param) {
        try {
            msgSceneParamMapper.insert(param);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, PARAM_DUPLICATE_MESSAGE);
        }
    }

    private void updateParam(MsgSceneParam param) {
        try {
            msgSceneParamMapper.updateById(param);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, PARAM_DUPLICATE_MESSAGE);
        }
    }

    private void requireScene(Long sceneId) {
        MsgScene scene = msgSceneMapper.selectById(sceneId);
        if (scene == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, SCENE_NOT_FOUND_MESSAGE);
        }
    }

    private MsgSceneParam requireParam(Long sceneId, Long paramId) {
        MsgSceneParam param = msgSceneParamMapper.selectById(paramId);
        if (param == null || !Objects.equals(param.getSceneId(), sceneId)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, PARAM_NOT_FOUND_MESSAGE);
        }
        return param;
    }

    private void validateParamName(String paramName) {
        MessageCenterValidator.requireValidParamName(paramName);
    }

    private void validateParamLabel(String paramLabel) {
        if (!StringUtils.hasText(paramLabel) || paramLabel.length() > 20) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参数标签不能为空且长度不能超过20");
        }
    }

    private void validateParamType(String paramType) {
        if (!StringUtils.hasText(paramType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参数类型不能为空");
        }
        try {
            ParamType.fromCode(paramType);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参数类型不合法");
        }
    }

    private Integer resolveIsRequired(Integer isRequired) {
        Integer value = isRequired == null ? NOT_REQUIRED : isRequired;
        if (value != NOT_REQUIRED && value != REQUIRED) {
            throw new BizException(ErrorCode.PARAM_ERROR, "isRequired只能为0或1");
        }
        return value;
    }

    private Integer resolveCreateSortOrder(Long sceneId, Integer sortOrder) {
        if (sortOrder != null) {
            validateSortOrder(sortOrder);
            return sortOrder;
        }
        Integer maxSortOrder = msgSceneParamMapper.selectMaxSortOrder(sceneId);
        return (maxSortOrder == null ? 0 : maxSortOrder) + 1;
    }

    private Integer resolveUpdateSortOrder(Integer sortOrder, Integer currentSortOrder) {
        if (sortOrder == null) {
            return currentSortOrder;
        }
        validateSortOrder(sortOrder);
        return sortOrder;
    }

    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "sortOrder必须为正整数");
        }
    }

    private void ensureParamNameUnique(Long sceneId, String paramName, Long excludeId) {
        LambdaQueryWrapper<MsgSceneParam> wrapper = new LambdaQueryWrapper<MsgSceneParam>()
                .eq(MsgSceneParam::getSceneId, sceneId)
                .eq(MsgSceneParam::getParamName, paramName)
                .ne(excludeId != null, MsgSceneParam::getId, excludeId);
        Long count = msgSceneParamMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DATA_DUPLICATE, PARAM_DUPLICATE_MESSAGE);
        }
    }

    private void ensureUsageAllowsUpdate(MsgSceneParam existed, SceneParamUpdateDTO request) {
        SceneParamUsageVO usage = normalizeUsage(sceneParamUsageChecker.checkUsage(existed));
        if (Boolean.TRUE.equals(usage.getUsed())
                && (!Objects.equals(existed.getParamName(), request.getParamName())
                || !Objects.equals(existed.getParamType(), request.getParamType()))) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED, "参数已被引用，不能修改参数名或参数类型");
        }
    }

    private void validateSortRequest(SceneParamSortDTO request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "排序列表不能为空");
        }
        Set<Long> paramIds = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        for (SceneParamSortItemDTO item : request.getItems()) {
            if (item == null || item.getParamId() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "paramId不能为空");
            }
            validateSortOrder(item.getSortOrder());
            if (!paramIds.add(item.getParamId())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "paramId不能重复");
            }
            if (!sortOrders.add(item.getSortOrder())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "sortOrder不能重复");
            }
        }
    }

    private SceneParamUsageVO normalizeUsage(SceneParamUsageVO usage) {
        return usage == null ? SceneParamUsageVO.unused() : usage;
    }

    private SceneParamVO toVO(MsgSceneParam param) {
        return toVO(param, null);
    }

    private SceneParamVO toVO(MsgSceneParam param, SceneParamUsageIndex usageIndex) {
        SceneParamVO vo = new SceneParamVO();
        vo.setId(param.getId());
        vo.setSceneId(param.getSceneId());
        vo.setParamName(param.getParamName());
        vo.setParamLabel(param.getParamLabel());
        vo.setParamType(param.getParamType());
        vo.setParamTypeDesc(ParamType.fromCode(param.getParamType()).getDesc());
        vo.setSortOrder(param.getSortOrder());
        vo.setIsRequired(param.getIsRequired() == null ? NOT_REQUIRED : param.getIsRequired());
        vo.setUsageCount(usageIndex == null ? 0L : (long) usageIndex.getTemplates(param.getId()).size());
        vo.setCreatedAt(param.getCreateTime());
        vo.setUpdatedAt(param.getUpdateTime());
        return vo;
    }
}
