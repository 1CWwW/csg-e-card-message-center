package com.csg.ecard.messagecenter.module.scene.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamSortDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamSortItemDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.scene.service.impl.SceneParamServiceImpl;
import com.csg.ecard.messagecenter.module.scene.usage.SceneParamUsageChecker;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class SceneParamServiceImplTest {

    @Mock
    private MsgSceneMapper msgSceneMapper;

    @Mock
    private MsgSceneParamMapper msgSceneParamMapper;

    @Mock
    private SceneParamUsageChecker sceneParamUsageChecker;

    @InjectMocks
    private SceneParamServiceImpl sceneParamService;

    @Test
    void shouldFailWhenSceneNotFound() {
        when(msgSceneMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> sceneParamService.create(1L, createRequest("merchantName")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void shouldRejectInvalidAndReservedParamName() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));

        assertThatThrownBy(() -> sceneParamService.create(1L, createRequest("1merchant")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_NAME_INVALID.getCode()));

        assertThatThrownBy(() -> sceneParamService.create(1L, createRequest("Function")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_NAME_INVALID.getCode()));
    }

    @Test
    void shouldRejectDuplicateParamNameInSameScene() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> sceneParamService.create(1L, createRequest("merchantName")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_DUPLICATE.getCode()));
    }

    @Test
    void shouldAllowSameParamNameInDifferentScene() {
        when(msgSceneMapper.selectById(2L)).thenReturn(scene(2L));
        when(msgSceneParamMapper.selectMaxSortOrder(2L)).thenReturn(0);
        when(msgSceneParamMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.insert(any(MsgSceneParam.class))).thenAnswer(invocation -> {
            MsgSceneParam param = invocation.getArgument(0);
            param.setId(10L);
            return 1;
        });

        SceneParamVO result = sceneParamService.create(2L, createRequest("merchantName"));

        assertThat(result.getSceneId()).isEqualTo(2L);
        assertThat(result.getParamName()).isEqualTo("merchantName");
    }

    @Test
    void shouldGenerateSortOrderByMaxPlusOne() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectMaxSortOrder(1L)).thenReturn(5);
        when(msgSceneParamMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.insert(any(MsgSceneParam.class))).thenReturn(1);

        sceneParamService.create(1L, createRequest("merchantName"));

        ArgumentCaptor<MsgSceneParam> captor = ArgumentCaptor.forClass(MsgSceneParam.class);
        verify(msgSceneParamMapper).insert(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(6);
    }

    @Test
    void shouldDefaultIsRequiredToZero() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectMaxSortOrder(1L)).thenReturn(0);
        when(msgSceneParamMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.insert(any(MsgSceneParam.class))).thenReturn(1);

        sceneParamService.create(1L, createRequest("merchantName"));

        ArgumentCaptor<MsgSceneParam> captor = ArgumentCaptor.forClass(MsgSceneParam.class);
        verify(msgSceneParamMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsRequired()).isZero();
    }

    @Test
    void shouldRejectInvalidIsRequired() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        SceneParamCreateDTO request = createRequest("merchantName");
        request.setIsRequired(2);

        assertThatThrownBy(() -> sceneParamService.create(1L, request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectCrossSceneUpdateAndDelete() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectById(20L)).thenReturn(param(20L, 2L, "merchantName"));

        assertThatThrownBy(() -> sceneParamService.update(1L, 20L, updateRequest("merchantName")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));

        assertThatThrownBy(() -> sceneParamService.delete(1L, 20L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void shouldSortParamsInSameTransactionAfterValidation() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                param(1L, 1L, "merchantName"),
                param(2L, 1L, "amount")
        ));

        sceneParamService.sort(1L, sortRequest(item(1L, 2), item(2L, 1)));

        verify(msgSceneParamMapper, times(2)).updateById(any(MsgSceneParam.class));
    }

    @Test
    void shouldRejectSortRequestAndAvoidPartialUpdate() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));

        assertThatThrownBy(() -> sceneParamService.sort(1L, sortRequest(item(1L, 1), item(2L, 1))))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(msgSceneParamMapper, never()).updateById(any(MsgSceneParam.class));
    }

    @Test
    void shouldRejectSortWhenParamNotBelongToScene() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(param(1L, 1L, "merchantName")));

        assertThatThrownBy(() -> sceneParamService.sort(1L, sortRequest(item(1L, 1), item(2L, 2))))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(msgSceneParamMapper, never()).updateById(any(MsgSceneParam.class));
    }

    @Test
    void shouldReturnUnusedUsageByDefault() {
        MsgSceneParam param = param(1L, 1L, "merchantName");
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectById(1L)).thenReturn(param);
        when(sceneParamUsageChecker.checkUsage(param)).thenReturn(SceneParamUsageVO.unused());

        SceneParamUsageVO usage = sceneParamService.usage(1L, 1L);

        assertThat(usage.getUsed()).isFalse();
        assertThat(usage.getUsageCount()).isZero();
        assertThat(usage.getTemplates()).isEmpty();
    }

    @Test
    void shouldRestrictUpdateAndDeleteWhenParamUsed() {
        MsgSceneParam param = param(1L, 1L, "merchantName");
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L));
        when(msgSceneParamMapper.selectById(1L)).thenReturn(param);
        when(sceneParamUsageChecker.checkUsage(param)).thenReturn(new SceneParamUsageVO(true, 1L, List.of("模板A")));

        assertThatThrownBy(() -> sceneParamService.update(1L, 1L, updateRequest("newName")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.STATUS_NOT_ALLOWED.getCode()));

        assertThatThrownBy(() -> sceneParamService.delete(1L, 1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DELETE_NOT_ALLOWED.getCode()));
    }

    private MsgScene scene(Long id) {
        MsgScene scene = new MsgScene();
        scene.setId(id);
        return scene;
    }

    private MsgSceneParam param(Long id, Long sceneId, String paramName) {
        MsgSceneParam param = new MsgSceneParam();
        param.setId(id);
        param.setSceneId(sceneId);
        param.setParamName(paramName);
        param.setParamLabel("商户名称");
        param.setParamType(ParamType.STRING.getCode());
        param.setSortOrder(1);
        param.setIsRequired(0);
        return param;
    }

    private SceneParamCreateDTO createRequest(String paramName) {
        SceneParamCreateDTO request = new SceneParamCreateDTO();
        request.setParamName(paramName);
        request.setParamLabel("商户名称");
        request.setParamType(ParamType.STRING.getCode());
        return request;
    }

    private SceneParamUpdateDTO updateRequest(String paramName) {
        SceneParamUpdateDTO request = new SceneParamUpdateDTO();
        request.setParamName(paramName);
        request.setParamLabel("商户名称");
        request.setParamType(ParamType.STRING.getCode());
        request.setSortOrder(1);
        request.setIsRequired(0);
        return request;
    }

    private SceneParamSortDTO sortRequest(SceneParamSortItemDTO... items) {
        SceneParamSortDTO request = new SceneParamSortDTO();
        request.setItems(List.of(items));
        return request;
    }

    private SceneParamSortItemDTO item(Long paramId, Integer sortOrder) {
        SceneParamSortItemDTO item = new SceneParamSortItemDTO();
        item.setParamId(paramId);
        item.setSortOrder(sortOrder);
        return item;
    }
}
