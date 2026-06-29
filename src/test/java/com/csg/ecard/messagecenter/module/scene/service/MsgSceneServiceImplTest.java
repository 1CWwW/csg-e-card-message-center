package com.csg.ecard.messagecenter.module.scene.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.config.mybatis.MybatisPlusConfig;
import com.csg.ecard.messagecenter.module.scene.dto.SceneCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.ScenePageQueryDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.enums.SceneModule;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.SceneParamCountResult;
import com.csg.ecard.messagecenter.module.scene.service.impl.MsgSceneServiceImpl;
import com.csg.ecard.messagecenter.module.scene.vo.MsgSceneVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class MsgSceneServiceImplTest {

    @Mock
    private MsgSceneMapper msgSceneMapper;

    @Mock
    private MsgSceneParamMapper msgSceneParamMapper;

    @InjectMocks
    private MsgSceneServiceImpl msgSceneService;

    @Test
    void shouldCreateSceneWhenSceneCodeValid() {
        SceneCreateDTO request = createRequest("CANTEEN_CONSUME_SUCCESS");
        when(msgSceneMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneMapper.insert(any(MsgScene.class))).thenAnswer(invocation -> {
            MsgScene scene = invocation.getArgument(0);
            LocalDateTime now = LocalDateTime.now();
            scene.setId(1L);
            scene.setCreateTime(now);
            scene.setUpdateTime(now);
            return 1;
        });

        MsgSceneVO result = msgSceneService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSceneCode()).isEqualTo("CANTEEN_CONSUME_SUCCESS");
        assertThat(result.getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getParamCount()).isZero();
        assertThat(result.getTemplateCount()).isZero();
        ArgumentCaptor<MsgScene> captor = ArgumentCaptor.forClass(MsgScene.class);
        verify(msgSceneMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
    }

    @Test
    void shouldDefaultCreateDtoStatusToEnable() {
        SceneCreateDTO request = new SceneCreateDTO();

        assertThat(request.getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
    }

    @Test
    void shouldCreateSceneWithExplicitEnableStatus() {
        SceneCreateDTO request = createRequest("CANTEEN_CONSUME_ENABLE");
        request.setStatus(CommonStatus.ENABLE.getCode());
        when(msgSceneMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneMapper.insert(any(MsgScene.class))).thenAnswer(invocation -> {
            MsgScene scene = invocation.getArgument(0);
            scene.setId(2L);
            return 1;
        });

        MsgSceneVO result = msgSceneService.create(request);

        ArgumentCaptor<MsgScene> captor = ArgumentCaptor.forClass(MsgScene.class);
        verify(msgSceneMapper).insert(captor.capture());
        assertThat(result.getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
    }

    @Test
    void shouldCreateSceneWithExplicitDisableStatus() {
        SceneCreateDTO request = createRequest("CANTEEN_CONSUME_DISABLE");
        request.setStatus(CommonStatus.DISABLE.getCode());
        when(msgSceneMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneMapper.insert(any(MsgScene.class))).thenAnswer(invocation -> {
            MsgScene scene = invocation.getArgument(0);
            scene.setId(3L);
            return 1;
        });

        MsgSceneVO result = msgSceneService.create(request);

        ArgumentCaptor<MsgScene> captor = ArgumentCaptor.forClass(MsgScene.class);
        verify(msgSceneMapper).insert(captor.capture());
        assertThat(result.getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
    }

    @Test
    void shouldRejectInvalidCreateStatus() {
        SceneCreateDTO request = createRequest("CANTEEN_CONSUME_INVALID_STATUS");
        request.setStatus(2);
        when(msgSceneMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> msgSceneService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldDefaultNullCreateStatusToEnable() {
        SceneCreateDTO request = createRequest("CANTEEN_CONSUME_NULL_STATUS");
        request.setStatus(null);
        when(msgSceneMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneMapper.insert(any(MsgScene.class))).thenAnswer(invocation -> {
            MsgScene scene = invocation.getArgument(0);
            scene.setId(4L);
            return 1;
        });

        MsgSceneVO result = msgSceneService.create(request);

        assertThat(result.getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
    }

    @Test
    void shouldRejectLowercaseStartSceneCode() {
        SceneCreateDTO request = createRequest("canteen_CONSUME");

        assertThatThrownBy(() -> msgSceneService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectNumberStartSceneCode() {
        SceneCreateDTO request = createRequest("1CANTEEN_CONSUME");

        assertThatThrownBy(() -> msgSceneService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectIllegalCharacterSceneCode() {
        SceneCreateDTO request = createRequest("CANTEEN-CONSUME");

        assertThatThrownBy(() -> msgSceneService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectDuplicateSceneCode() {
        SceneCreateDTO request = createRequest("CANTEEN_CONSUME_SUCCESS");
        when(msgSceneMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> msgSceneService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_DUPLICATE.getCode()));
    }

    @Test
    void shouldUpdateSceneCodeWhenUpdatingScene() {
        MsgScene existed = scene(10L, "OLD_CODE", CommonStatus.ENABLE.getCode());
        MsgScene updated = scene(10L, "NEW_CODE", CommonStatus.DISABLE.getCode());
        updated.setSceneName("新名称");
        when(msgSceneMapper.selectById(10L)).thenReturn(existed, updated);
        when(msgSceneMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneMapper.updateById(any(MsgScene.class))).thenReturn(1);
        SceneUpdateDTO request = updateRequest();
        request.setSceneCode("NEW_CODE");

        MsgSceneVO result = msgSceneService.update(10L, request);

        ArgumentCaptor<MsgScene> captor = ArgumentCaptor.forClass(MsgScene.class);
        verify(msgSceneMapper).updateById(captor.capture());
        assertThat(captor.getValue().getSceneCode()).isEqualTo("NEW_CODE");
        assertThat(captor.getValue().getUpdateTime()).isNull();
        assertThat(result.getSceneCode()).isEqualTo("NEW_CODE");
        assertThat(result.getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
        assertThat(result.getUpdatedAt()).isEqualTo(updated.getUpdateTime());
    }

    @Test
    void shouldBuildCorrectPageQueryConditions() {
        ScenePageQueryDTO query = new ScenePageQueryDTO();
        query.setPageNum(2);
        query.setPageSize(20);
        query.setSceneCode("CANTEEN_CONSUME_SUCCESS");
        query.setSceneName("消费");
        query.setModule(SceneModule.CANTEEN_CONSUME.getCode());
        query.setStatus(1);
        Page<MsgScene> mapperResult = new Page<>(2L, 20L);
        mapperResult.setTotal(1L);
        mapperResult.setRecords(List.of(scene(1L, "CANTEEN_CONSUME_SUCCESS", CommonStatus.ENABLE.getCode())));
        when(msgSceneMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mapperResult);
        SceneParamCountResult countResult = new SceneParamCountResult();
        countResult.setSceneId(1L);
        countResult.setParamCount(3L);
        when(msgSceneParamMapper.selectParamCountsBySceneIds(any())).thenReturn(List.of(countResult));

        PageResult<MsgSceneVO> result = msgSceneService.page(query);

        ArgumentCaptor<Page<MsgScene>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<MsgScene>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(msgSceneMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
        assertThat(result.getList()).hasSize(1);
        MsgSceneVO record = result.getList().get(0);
        assertThat(record.getCreatedAt()).isNotNull();
        assertThat(record.getUpdatedAt()).isNotNull();
        assertThat(record.getParamCount()).isEqualTo(3L);
        assertThat(record.getTemplateCount()).isZero();
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("scene_code");
        assertThat(sqlSegment).contains("scene_name");
        assertThat(sqlSegment).contains("module");
        assertThat(sqlSegment).contains("status");
        assertThat(sqlSegment).contains("create_time");
    }

    @Test
    void shouldRejectInvalidIntegerStatusWhenPagingScene() {
        ScenePageQueryDTO query = new ScenePageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setStatus(2);

        assertThatThrownBy(() -> msgSceneService.page(query))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldDefaultScenePageSizeToTwenty() {
        ScenePageQueryDTO query = new ScenePageQueryDTO();

        assertThat(query.getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldRejectScenePageSizeGreaterThanOneHundred() {
        ScenePageQueryDTO query = new ScenePageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(101);

        assertThatThrownBy(() -> msgSceneService.page(query))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldThrowBizExceptionWhenSceneNotFound() {
        when(msgSceneMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> msgSceneService.detail(404L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void shouldReturnCorrectStatusWhenQueryingSceneDetail() {
        when(msgSceneMapper.selectById(5L)).thenReturn(scene(5L, "CODE_QUERY_STATUS", CommonStatus.DISABLE.getCode()));

        MsgSceneVO result = msgSceneService.detail(5L);

        assertThat(result.getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
    }

    @Test
    void shouldReturnEnabledTemplateCountWhenCheckingDisable() {
        when(msgSceneMapper.selectById(5L)).thenReturn(scene(5L, "CODE_DISABLE_CHECK", CommonStatus.ENABLE.getCode()));
        when(msgSceneMapper.selectEnabledTemplateCountBySceneId(5L)).thenReturn(3L);

        var result = msgSceneService.disableCheck(5L);

        assertThat(result.getEnabledTemplateCount()).isEqualTo(3L);
    }

    @Test
    void shouldToggleSceneStatus() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L, "CODE_ENABLE", CommonStatus.ENABLE.getCode()));
        when(msgSceneMapper.selectById(2L)).thenReturn(scene(2L, "CODE_DISABLE", CommonStatus.DISABLE.getCode()));
        when(msgSceneMapper.updateById(any(MsgScene.class))).thenReturn(1);

        MsgSceneVO disabled = msgSceneService.toggle(1L);
        MsgSceneVO enabled = msgSceneService.toggle(2L);

        assertThat(disabled.getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
        assertThat(enabled.getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
    }

    @Test
    void shouldDeleteScene() {
        when(msgSceneMapper.selectById(1L)).thenReturn(scene(1L, "CODE_DELETE", CommonStatus.ENABLE.getCode()));
        when(msgSceneMapper.deleteById(eq(1L))).thenReturn(1);

        msgSceneService.delete(1L);

        verify(msgSceneMapper).deleteById(1L);
    }

    @Test
    void shouldAutoFillCreateTimeAndUpdateTimeWhenInsertingScene() {
        MetaObjectHandler handler = new MybatisPlusConfig().metaObjectHandler();
        MsgScene scene = new MsgScene();

        handler.insertFill(SystemMetaObject.forObject(scene));

        assertThat(scene.getCreateTime()).isNotNull();
        assertThat(scene.getUpdateTime()).isNotNull();
    }

    @Test
    void shouldAutoFillUpdateTimeWhenUpdatingScene() {
        MetaObjectHandler handler = new MybatisPlusConfig().metaObjectHandler();
        MsgScene scene = new MsgScene();
        scene.setId(1L);

        handler.updateFill(SystemMetaObject.forObject(scene));

        assertThat(scene.getUpdateTime()).isNotNull();
    }

    private SceneCreateDTO createRequest(String sceneCode) {
        SceneCreateDTO request = new SceneCreateDTO();
        request.setSceneCode(sceneCode);
        request.setSceneName("食堂消费成功");
        request.setModule(SceneModule.CANTEEN_CONSUME.getCode());
        request.setDescription("消费成功后发送消息");
        return request;
    }

    private SceneUpdateDTO updateRequest() {
        SceneUpdateDTO request = new SceneUpdateDTO();
        request.setSceneCode("UPDATED_CODE");
        request.setSceneName("新名称");
        request.setModule(SceneModule.ORDER_MANAGEMENT.getCode());
        request.setDescription("更新描述");
        request.setStatus(CommonStatus.DISABLE.getCode());
        return request;
    }

    private MsgScene scene(Long id, String sceneCode, Integer status) {
        MsgScene scene = new MsgScene();
        LocalDateTime now = LocalDateTime.now();
        scene.setId(id);
        scene.setSceneCode(sceneCode);
        scene.setSceneName("食堂消费成功");
        scene.setModule(SceneModule.CANTEEN_CONSUME.getCode());
        scene.setDescription("消费成功后发送消息");
        scene.setStatus(status);
        scene.setCreateTime(now.minusMinutes(1));
        scene.setUpdateTime(now);
        return scene;
    }
}
