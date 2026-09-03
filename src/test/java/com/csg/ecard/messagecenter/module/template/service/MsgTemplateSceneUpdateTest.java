package com.csg.ecard.messagecenter.module.template.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationResult;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplateUnit;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.mapper.TemplateQueryRow;
import com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl;
import com.csg.ecard.messagecenter.module.template.vo.TemplateDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateToolboxVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 模板所属场景编辑回归测试。
 */
@ExtendWith(MockitoExtension.class)
class MsgTemplateSceneUpdateTest {

    private static final Long TEMPLATE_ID = 10L;
    private static final Long SOURCE_SCENE_ID = 1L;
    private static final Long TARGET_SCENE_ID = 2L;

    @Mock
    private MsgTemplateMapper msgTemplateMapper;
    @Mock
    private MsgTemplateUnitMapper msgTemplateUnitMapper;
    @Mock
    private MsgSceneMapper msgSceneMapper;
    @Mock
    private MsgSceneParamMapper msgSceneParamMapper;
    @Mock
    private BlocklyJsonValidator blocklyJsonValidator;
    @Mock
    private BlocklyRenderer blocklyRenderer;

    private MsgTemplateServiceImpl msgTemplateService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MsgTemplate.class);
        TableInfoHelper.initTableInfo(assistant, MsgSceneParam.class);
        objectMapper = new ObjectMapper();
        msgTemplateService = new MsgTemplateServiceImpl(
                msgTemplateMapper,
                msgTemplateUnitMapper,
                msgSceneMapper,
                msgSceneParamMapper,
                blocklyJsonValidator,
                blocklyRenderer);
    }

    @Test
    void shouldExposeNewSceneInUpdateListDetailAndToolboxWithoutChangingBlocklyJson() throws Exception {
        String blocklyJson = "{\"schemaVersion\":1,\"workspace\":{\"sourceSceneId\":\"1\"}}";
        JsonNode parsedBlocklyJson = objectMapper.readTree(blocklyJson);
        AtomicReference<MsgTemplate> state = new AtomicReference<>(template(SOURCE_SCENE_ID, blocklyJson));
        MsgScene targetScene = enabledScene(TARGET_SCENE_ID, "SCENE_NEW", "新场景");
        MsgSceneParam targetParam = sceneParam(9007199254740993L, TARGET_SCENE_ID,
                "newAmount", "新场景金额");
        stubMutableTemplate(state);
        when(msgSceneMapper.selectById(TARGET_SCENE_ID)).thenReturn(targetScene);
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateMapper.selectTemplateDetail(TEMPLATE_ID))
                .thenAnswer(invocation -> queryRow(state.get(), targetScene));
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(TEMPLATE_ID)).thenReturn(List.of());
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of(targetParam));
        when(blocklyJsonValidator.validateStored(anyString(), eq(TARGET_SCENE_ID), anyMap(), any()))
                .thenReturn(new BlocklyValidationResult(
                        parsedBlocklyJson, true, false, List.of("原场景参数引用已失效"), Set.of(), Map.of()));
        when(msgTemplateMapper.selectTemplatePage(any(), any())).thenAnswer(invocation -> {
            Page<TemplateQueryRow> page = invocation.getArgument(0);
            page.setRecords(List.of(queryRow(state.get(), targetScene)));
            page.setTotal(1L);
            return page;
        });

        TemplateDetailVO updated = msgTemplateService.update(
                TEMPLATE_ID, updateRequest(TARGET_SCENE_ID, "更新后的模板", List.of()));
        PageResult<TemplateListVO> page = msgTemplateService.page(pageQuery());
        TemplateDetailVO detail = msgTemplateService.detail(TEMPLATE_ID);
        TemplateToolboxVO toolbox = msgTemplateService.toolbox(TEMPLATE_ID);

        assertScene(updated, targetScene);
        assertThat(updated.getSceneParams()).singleElement().satisfies(param -> {
            assertThat(param.getId()).isEqualTo(targetParam.getId());
            assertThat(param.getSceneId()).isEqualTo(TARGET_SCENE_ID);
        });
        assertThat(objectMapper.valueToTree(updated).path("sceneId").isTextual()).isTrue();
        assertThat(objectMapper.valueToTree(updated).path("sceneId").asText()).isEqualTo("2");
        assertThat(page.getList()).singleElement().satisfies(item -> assertScene(item, targetScene));
        assertScene(detail, targetScene);
        assertThat(detail.getSceneParams()).extracting("id").containsExactly(targetParam.getId());
        assertThat(toolbox.getSceneId()).isEqualTo(TARGET_SCENE_ID);
        assertThat(toolbox.getParams()).singleElement().satisfies(param -> {
            assertThat(param.getParamId()).isEqualTo(targetParam.getId());
            assertThat(param.getParamName()).isEqualTo(targetParam.getParamName());
        });
        assertThat(state.get().getBlocklyJson()).isEqualTo(blocklyJson);
        assertThat(detail.getBlocklyJson()).isEqualTo(parsedBlocklyJson);

        ArgumentCaptor<MsgTemplate> updateCaptor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSceneId()).isEqualTo(TARGET_SCENE_ID);
        assertThat(updateCaptor.getValue().getBlocklyJson()).isNull();
    }

    @Test
    void shouldKeepOriginalSceneWhenOnlyOtherBasicFieldsChange() {
        AtomicReference<MsgTemplate> state = new AtomicReference<>(template(SOURCE_SCENE_ID, null));
        MsgScene sourceScene = enabledScene(SOURCE_SCENE_ID, "SCENE_OLD", "原场景");
        stubMutableTemplate(state);
        when(msgSceneMapper.selectById(SOURCE_SCENE_ID)).thenReturn(sourceScene);
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateMapper.selectTemplateDetail(TEMPLATE_ID))
                .thenAnswer(invocation -> queryRow(state.get(), sourceScene));
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(TEMPLATE_ID)).thenReturn(List.of());
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());

        TemplateDetailVO result = msgTemplateService.update(
                TEMPLATE_ID, updateRequest(SOURCE_SCENE_ID, "仅修改名称", List.of()));

        assertThat(result.getTemplateName()).isEqualTo("仅修改名称");
        assertScene(result, sourceScene);
        assertThat(state.get().getSceneId()).isEqualTo(SOURCE_SCENE_ID);
    }

    @Test
    void shouldRejectMissingTargetScene() {
        when(msgTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template(SOURCE_SCENE_ID, null));
        when(msgSceneMapper.selectById(TARGET_SCENE_ID)).thenReturn(null);

        assertThatThrownBy(() -> msgTemplateService.update(
                TEMPLATE_ID, updateRequest(TARGET_SCENE_ID, "新名称", List.of())))
                .isInstanceOf(BizException.class)
                .hasMessage("场景不存在")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());

        verify(msgTemplateMapper, never()).updateById(any(MsgTemplate.class));
    }

    @Test
    void shouldRejectDisabledTargetSceneUsingCreateValidationRule() {
        MsgScene disabledScene = enabledScene(TARGET_SCENE_ID, "SCENE_DISABLED", "停用场景");
        disabledScene.setStatus(CommonStatus.DISABLE.getCode());
        when(msgTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template(SOURCE_SCENE_ID, null));
        when(msgSceneMapper.selectById(TARGET_SCENE_ID)).thenReturn(disabledScene);

        assertThatThrownBy(() -> msgTemplateService.update(
                TEMPLATE_ID, updateRequest(TARGET_SCENE_ID, "新名称", List.of())))
                .isInstanceOf(BizException.class)
                .hasMessage("只能选择启用场景")
                .extracting("code")
                .isEqualTo(ErrorCode.STATUS_NOT_ALLOWED.getCode());

        verify(msgTemplateMapper, never()).updateById(any(MsgTemplate.class));
    }

    @Test
    void shouldRejectDuplicateNameInTargetSceneAndExcludeCurrentTemplate() {
        when(msgTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template(SOURCE_SCENE_ID, null));
        when(msgSceneMapper.selectById(TARGET_SCENE_ID))
                .thenReturn(enabledScene(TARGET_SCENE_ID, "SCENE_NEW", "新场景"));
        when(msgTemplateMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> msgTemplateService.update(
                TEMPLATE_ID, updateRequest(TARGET_SCENE_ID, "目标场景重名", List.of())))
                .isInstanceOf(BizException.class)
                .hasMessage("当前场景下模板名称已存在")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_DUPLICATE.getCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<MsgTemplate>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(msgTemplateMapper).selectCount(wrapperCaptor.capture());
        wrapperCaptor.getValue().getSqlSegment();
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains(TARGET_SCENE_ID, "目标场景重名", TEMPLATE_ID)
                .doesNotContain(SOURCE_SCENE_ID);
        verify(msgTemplateMapper, never()).updateById(any(MsgTemplate.class));
    }

    @Test
    void shouldRollbackTemplateAndUnitChangesWhenUnitSaveFails() {
        AtomicReference<MsgTemplate> state = new AtomicReference<>(template(SOURCE_SCENE_ID, null));
        AtomicReference<List<String>> unitState = new AtomicReference<>(new ArrayList<>(List.of("old-unit")));
        AtomicReference<MsgTemplate> templateSnapshot = new AtomicReference<>();
        AtomicReference<List<String>> unitSnapshot = new AtomicReference<>();
        stubMutableTemplate(state);
        when(msgSceneMapper.selectById(TARGET_SCENE_ID))
                .thenReturn(enabledScene(TARGET_SCENE_ID, "SCENE_NEW", "新场景"));
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateUnitMapper.deleteByTemplateId(TEMPLATE_ID)).thenAnswer(invocation -> {
            unitState.set(new ArrayList<>());
            return 1;
        });
        doThrow(new IllegalStateException("unit insert failed"))
                .when(msgTemplateUnitMapper).insert(any(MsgTemplateUnit.class));

        PlatformTransactionManager transactionManager = org.mockito.Mockito.mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = org.mockito.Mockito.mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            templateSnapshot.set(copyTemplate(state.get()));
            unitSnapshot.set(new ArrayList<>(unitState.get()));
            return transactionStatus;
        });
        doAnswer(invocation -> {
            state.set(copyTemplate(templateSnapshot.get()));
            unitState.set(new ArrayList<>(unitSnapshot.get()));
            return null;
        }).when(transactionManager).rollback(transactionStatus);
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(msgTemplateService);
        proxyFactory.setInterfaces(MsgTemplateService.class);
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager, new AnnotationTransactionAttributeSource()));
        MsgTemplateService transactionalService = (MsgTemplateService) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactionalService.update(
                TEMPLATE_ID, updateRequest(TARGET_SCENE_ID, "更新后的模板", List.of("new-unit"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unit insert failed");

        assertThat(state.get().getSceneId()).isEqualTo(SOURCE_SCENE_ID);
        assertThat(state.get().getTemplateName()).isEqualTo("原模板");
        assertThat(unitState.get()).containsExactly("old-unit");
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void shouldRequireStringSceneIdInUpdateContractWithoutPrecisionLoss() throws Exception {
        Field sceneIdField = TemplateUpdateDTO.class.getDeclaredField("sceneId");
        NotNull notNull = sceneIdField.getAnnotation(NotNull.class);
        Schema schema = sceneIdField.getAnnotation(Schema.class);

        assertThat(notNull).isNotNull();
        assertThat(notNull.message()).isEqualTo("场景ID不能为空");
        assertThat(schema).isNotNull();
        assertThat(schema.type()).isEqualTo("string");
        assertThat(schema.requiredMode()).isEqualTo(Schema.RequiredMode.REQUIRED);

        TemplateUpdateDTO request = objectMapper.readValue("""
                {"templateName":"模板","sceneId":"2085629226374467586",\
                 "channelType":"SMS","status":0,"unitIds":[]}
                """, TemplateUpdateDTO.class);
        assertThat(request.getSceneId()).isEqualTo(2085629226374467586L);
    }

    private void stubMutableTemplate(AtomicReference<MsgTemplate> state) {
        when(msgTemplateMapper.selectById(TEMPLATE_ID)).thenAnswer(invocation -> state.get());
        when(msgTemplateMapper.updateById(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate update = invocation.getArgument(0);
            MsgTemplate current = state.get();
            current.setTemplateName(update.getTemplateName());
            current.setSceneId(update.getSceneId());
            current.setChannelType(update.getChannelType());
            current.setStatus(update.getStatus());
            return 1;
        });
    }

    private MsgTemplate template(Long sceneId, String blocklyJson) {
        MsgTemplate template = new MsgTemplate();
        template.setId(TEMPLATE_ID);
        template.setTemplateName("原模板");
        template.setSceneId(sceneId);
        template.setChannelType(ChannelType.SMS.getCode());
        template.setBlocklyJson(blocklyJson);
        template.setStatus(CommonStatus.DISABLE.getCode());
        return template;
    }

    private MsgTemplate copyTemplate(MsgTemplate source) {
        MsgTemplate copy = new MsgTemplate();
        copy.setId(source.getId());
        copy.setTemplateName(source.getTemplateName());
        copy.setSceneId(source.getSceneId());
        copy.setChannelType(source.getChannelType());
        copy.setBlocklyJson(source.getBlocklyJson());
        copy.setStatus(source.getStatus());
        return copy;
    }

    private TemplateUpdateDTO updateRequest(Long sceneId, String templateName, List<String> unitIds) {
        TemplateUpdateDTO request = new TemplateUpdateDTO();
        request.setTemplateName(templateName);
        request.setSceneId(sceneId);
        request.setChannelType(ChannelType.SMS.getCode());
        request.setStatus(CommonStatus.DISABLE.getCode());
        request.setUnitIds(unitIds);
        return request;
    }

    private TemplatePageQueryDTO pageQuery() {
        TemplatePageQueryDTO query = new TemplatePageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setContentStatus(0);
        return query;
    }

    private MsgScene enabledScene(Long id, String sceneCode, String sceneName) {
        MsgScene scene = new MsgScene();
        scene.setId(id);
        scene.setSceneCode(sceneCode);
        scene.setSceneName(sceneName);
        scene.setStatus(CommonStatus.ENABLE.getCode());
        return scene;
    }

    private MsgSceneParam sceneParam(Long id, Long sceneId, String paramName, String paramLabel) {
        MsgSceneParam param = new MsgSceneParam();
        param.setId(id);
        param.setSceneId(sceneId);
        param.setParamName(paramName);
        param.setParamLabel(paramLabel);
        param.setParamType(ParamType.NUMBER.getCode());
        param.setSortOrder(1);
        param.setIsRequired(1);
        return param;
    }

    private TemplateQueryRow queryRow(MsgTemplate template, MsgScene scene) {
        TemplateQueryRow row = new TemplateQueryRow();
        row.setId(template.getId());
        row.setTemplateName(template.getTemplateName());
        row.setSceneId(template.getSceneId());
        row.setSceneCode(scene.getSceneCode());
        row.setSceneName(scene.getSceneName());
        row.setChannelType(template.getChannelType());
        row.setBlocklyJson(template.getBlocklyJson());
        row.setStatus(template.getStatus());
        row.setUnitCount(0L);
        return row;
    }

    private void assertScene(TemplateListVO result, MsgScene expectedScene) {
        assertThat(result.getSceneId()).isEqualTo(expectedScene.getId());
        assertThat(result.getSceneCode()).isEqualTo(expectedScene.getSceneCode());
        assertThat(result.getSceneName()).isEqualTo(expectedScene.getSceneName());
    }
}
