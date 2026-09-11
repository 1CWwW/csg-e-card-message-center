package com.csg.ecard.messagecenter.module.template.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.enums.ParamType;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlockRenderContext;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyBlockTypes;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderResult;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationMode;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationResult;
import com.csg.ecard.messagecenter.module.template.blockly.SceneParamValueValidator;
import com.csg.ecard.messagecenter.module.template.dto.TemplateContentSaveDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCopyDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCreateDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.mapper.TemplateQueryRow;
import com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl;
import com.csg.ecard.messagecenter.module.template.vo.TemplateFilterOptionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息模板服务及 Blockly 表达式单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MsgTemplateServiceImplTest {

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

    @InjectMocks
    private MsgTemplateServiceImpl msgTemplateService;

    private MsgTemplate existed;
    private ObjectMapper objectMapper;
    private BlocklyJsonValidator actualValidator;
    private BlocklyRenderer actualRenderer;
    private Map<Long, MsgSceneParam> expressionParams;

    @BeforeEach
    void setUp() {
        existed = new MsgTemplate();
        existed.setId(10L);
        existed.setSceneId(1L);
        existed.setChannelType(ChannelType.SMS.getCode());
        existed.setTemplateName("原模板");
        existed.setStatus(CommonStatus.DISABLE.getCode());

        objectMapper = new ObjectMapper();
        actualValidator = new BlocklyJsonValidator(objectMapper);
        actualRenderer = new BlocklyRenderer(new SceneParamValueValidator());
        expressionParams = new LinkedHashMap<>();
        expressionParams.put(1L, param(1L, "left", ParamType.NUMBER));
        expressionParams.put(2L, param(2L, "right", ParamType.NUMBER));
        expressionParams.put(3L, param(3L, "zero", ParamType.NUMBER));
        expressionParams.put(4L, param(4L, "sendTime", ParamType.TIME));
        expressionParams.put(5L, param(5L, "branch0", ParamType.STRING));
        expressionParams.put(6L, param(6L, "branch1", ParamType.STRING));
        expressionParams.put(7L, param(7L, "elseBranch", ParamType.STRING));
        expressionParams.put(8L, optionalParam(8L, "stringItems", ParamType.STRING_ARRAY));
        expressionParams.put(9L, optionalParam(9L, "numberItems", ParamType.NUMBER_ARRAY));
        expressionParams.put(10L, param(10L, "prefix", ParamType.STRING));
        expressionParams.put(11L, optionalParam(11L, "wallets", ParamType.OBJECT_ARRAY));
        expressionParams.put(12L, param(12L, "isPark", ParamType.BOOLEAN));
        expressionParams.put(13L, optionalParam(13L, "optionalAmount", ParamType.NUMBER));
        expressionParams.put(14L, optionalParam(14L, "optionalTime", ParamType.TIME));
    }

    @Test
    void shouldAllowChannelTypeChangeWhenEditingTemplate() {
        TemplateUpdateDTO request = new TemplateUpdateDTO();
        request.setTemplateName("template");
        request.setSceneId(1L);
        request.setChannelType(ChannelType.ELINK.getCode());
        request.setStatus(CommonStatus.DISABLE.getCode());

        TemplateQueryRow saved = new TemplateQueryRow();
        saved.setId(10L);
        saved.setTemplateName("template");
        saved.setSceneId(1L);
        saved.setChannelType(ChannelType.ELINK.getCode());
        saved.setStatus(CommonStatus.DISABLE.getCode());

        when(msgTemplateMapper.selectById(10L)).thenReturn(existed);
        when(msgSceneMapper.selectById(1L)).thenReturn(enabledScene(1L));
        when(msgTemplateMapper.updateById(any(MsgTemplate.class))).thenReturn(1);
        when(msgTemplateMapper.selectTemplateDetail(10L)).thenReturn(saved);
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(10L)).thenReturn(List.of());

        var result = msgTemplateService.update(10L, request);

        ArgumentCaptor<MsgTemplate> updateCaptor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getChannelType()).isEqualTo(ChannelType.ELINK.getCode());
        assertThat(result.getChannelType()).isEqualTo(ChannelType.ELINK.getCode());
        assertThat(result.getChannelTypeDesc()).isEqualTo(ChannelType.ELINK.getDesc());
    }

    @Test
    void shouldRejectDuplicateTemplateNameInSameSceneWhenUpdating() {
        TemplateUpdateDTO request = new TemplateUpdateDTO();
        request.setSceneId(1L);
        request.setChannelType(ChannelType.SMS.getCode());
        request.setTemplateName("同场景模板");
        request.setStatus(CommonStatus.DISABLE.getCode());

        when(msgTemplateMapper.selectById(10L)).thenReturn(existed);
        when(msgSceneMapper.selectById(1L)).thenReturn(enabledScene(1L));
        when(msgTemplateMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> msgTemplateService.update(10L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("当前场景下模板名称已存在")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_DUPLICATE.getCode());

        verify(msgTemplateMapper, never()).updateById(any(MsgTemplate.class));
        verify(msgTemplateUnitMapper, never()).deleteByTemplateId(any());
    }

    @Test
    void shouldReturnSceneFilterOptionWithCodeNameStatusAndStringId() {
        MsgScene scene = new MsgScene();
        scene.setId(2085629226374467586L);
        scene.setSceneCode("A_01");
        scene.setSceneName("qqq");
        scene.setStatus(CommonStatus.ENABLE.getCode());
        when(msgSceneMapper.selectTemplateFilterOptions()).thenReturn(List.of(scene));

        List<TemplateFilterOptionVO> result = msgTemplateService.sceneFilterOptions();

        assertThat(result).singleElement().satisfies(option -> {
            assertThat(option.getValue()).isEqualTo("2085629226374467586");
            assertThat(option.getLabel()).isEqualTo("A_01 - qqq");
            assertThat(option.getSceneCode()).isEqualTo("A_01");
            assertThat(option.getSceneName()).isEqualTo("qqq");
            assertThat(option.getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
            assertThat(objectMapper.valueToTree(option).get("value").isTextual()).isTrue();
        });
    }

    @Test
    void shouldRejectMissingSceneWhenCreatingTemplate() {
        TemplateCreateDTO request = templateCreateRequest(1L);
        when(msgSceneMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> msgTemplateService.create(request))
                .isInstanceOf(BizException.class)
                .hasMessage("场景不存在")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());

        verify(msgTemplateMapper, never()).insert(any(MsgTemplate.class));
    }

    @Test
    void shouldRejectDisabledSceneWhenCreatingTemplate() {
        TemplateCreateDTO request = templateCreateRequest(1L);
        MsgScene scene = new MsgScene();
        scene.setId(1L);
        scene.setStatus(CommonStatus.DISABLE.getCode());
        when(msgSceneMapper.selectById(1L)).thenReturn(scene);

        assertThatThrownBy(() -> msgTemplateService.create(request))
                .isInstanceOf(BizException.class)
                .hasMessage("只能选择启用场景")
                .extracting("code")
                .isEqualTo(ErrorCode.STATUS_NOT_ALLOWED.getCode());

        verify(msgTemplateMapper, never()).insert(any(MsgTemplate.class));
    }

    @Test
    void shouldRebindBothSceneParamNodeTypesAcrossScenesWithStringIds() {
        MsgSceneParam sourceBalance = sceneParam(11L, 1L, "balance", "账户余额", ParamType.NUMBER);
        MsgSceneParam sourceName = sceneParam(12L, 1L, "customerName", "客户名称", ParamType.STRING);
        MsgSceneParam targetBalance = sceneParam(9007199254740993L, 2L,
                "balance", "目标账户余额", ParamType.NUMBER);
        MsgSceneParam targetName = sceneParam(9007199254740994L, 2L,
                "customerName", "目标客户名称", ParamType.STRING);
        ObjectNode balanceBlock = sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                "balance-node", "1", "11", "balance", "账户余额", ParamType.NUMBER);
        ObjectNode nameBlock = sceneParamBlock(BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF,
                "name-node", "1", "12", "customerName", "客户名称", ParamType.STRING);
        BlocklyValidationResult sourceValidation = actualValidator.validateWorkspace(
                1,
                linkedWorkspace(balanceBlock, nameBlock),
                1L,
                Map.of(11L, sourceBalance, 12L, sourceName),
                BlocklyValidationMode.DRAFT);

        BlocklyValidationResult rebound = actualValidator.rebindSceneParams(
                sourceValidation.getBlocklyJson(),
                Map.of(11L, sourceBalance, 12L, sourceName),
                2L,
                Map.of(targetBalance.getId(), targetBalance, targetName.getId(), targetName));

        assertThat(rebound.isValid()).isTrue();
        List<JsonNode> extraStates = rebound.getBlocklyJson().findValues("extraState");
        assertThat(extraStates).hasSize(2);
        assertReboundParam(extraStates.get(0), "2", "9007199254740993",
                "balance", "目标账户余额", ParamType.NUMBER);
        assertReboundParam(extraStates.get(1), "2", "9007199254740994",
                "customerName", "目标客户名称", ParamType.STRING);
    }

    @Test
    void shouldRejectCrossSceneCopyWhenTargetParamIsMissing() {
        MsgSceneParam sourceParam = sceneParam(11L, 1L, "balance", "账户余额", ParamType.NUMBER);
        ObjectNode block = sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                "balance-node", "1", "11", "balance", "账户余额", ParamType.NUMBER);
        BlocklyValidationResult sourceValidation = actualValidator.validateWorkspace(
                1, linkedWorkspace(block), 1L, Map.of(11L, sourceParam), BlocklyValidationMode.DRAFT);

        assertThatThrownBy(() -> actualValidator.rebindSceneParams(
                sourceValidation.getBlocklyJson(), Map.of(11L, sourceParam), 2L, Map.of()))
                .isInstanceOf(BizException.class)
                .hasMessage("参数‘账户余额’在目标场景中不存在");
    }

    @Test
    void shouldRejectCrossSceneCopyWhenTargetParamTypeDiffers() {
        MsgSceneParam sourceParam = sceneParam(11L, 1L, "amount", "消费金额", ParamType.NUMBER);
        MsgSceneParam targetParam = sceneParam(21L, 2L, "amount", "消费金额", ParamType.STRING);
        ObjectNode block = sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                "amount-node", "1", "11", "amount", "消费金额", ParamType.NUMBER);
        BlocklyValidationResult sourceValidation = actualValidator.validateWorkspace(
                1, linkedWorkspace(block), 1L, Map.of(11L, sourceParam), BlocklyValidationMode.DRAFT);

        assertThatThrownBy(() -> actualValidator.rebindSceneParams(
                sourceValidation.getBlocklyJson(), Map.of(11L, sourceParam),
                2L, Map.of(21L, targetParam)))
                .isInstanceOf(BizException.class)
                .hasMessage("参数‘消费金额’类型不一致：源场景为 NUMBER，目标场景为 STRING");
    }

    @Test
    void shouldCollectReadableErrorsForEachInvalidHistoricalParam() {
        ObjectNode balanceBlock = sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                "balance-node", "99", "11", "balance", "账户余额", ParamType.NUMBER);
        ObjectNode amountBlock = sceneParamBlock(BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF,
                "amount-node", "99", "12", "amount", "消费金额", ParamType.NUMBER);

        BlocklyValidationResult validation = actualValidator.validateWorkspace(
                1, linkedWorkspace(balanceBlock, amountBlock), 2L, Map.of(), BlocklyValidationMode.DRAFT);

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).containsExactly(
                "参数‘账户余额’引用场景与当前模板场景不一致",
                "参数‘消费金额’引用场景与当前模板场景不一致");
        assertThat(validation.getErrors()).allSatisfy(error -> {
            assertThat(error).doesNotContain(BlocklyBlockTypes.SCENE_PARAM_VALUE);
            assertThat(error).doesNotContain(BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF);
        });
    }

    @Test
    void shouldKeepOriginalParamBindingWhenTargetIdAndTypeDiffer() {
        initializeTemplateTableInfo();
        MsgSceneParam sourceParam = sceneParam(11L, 1L, "balance", "账户余额", ParamType.NUMBER);
        MsgSceneParam targetParam = sceneParam(9007199254740993L, 2L,
                "balance", "目标账户余额", ParamType.STRING);
        MsgTemplate source = sourceTemplate(1L, storedContent(1L, Map.of(11L, sourceParam),
                sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                        "balance-node", "1", "11", "balance", "账户余额", ParamType.NUMBER)));
        when(msgTemplateMapper.selectById(10L)).thenReturn(source);
        when(msgSceneMapper.selectById(2L)).thenReturn(enabledScene(2L));
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.selectList(any()))
                .thenReturn(List.of(sourceParam), List.of(targetParam));
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate copied = invocation.getArgument(0);
            copied.setId(20L);
            return 1;
        });

        var result = actualTemplateService().copy(10L, copyRequest(2L, true));

        assertThat(result.getNewTemplateId()).isEqualTo(20L);
        assertThat(result.getHasContent()).isFalse();
        ArgumentCaptor<MsgTemplate> captor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).insert(captor.capture());
        MsgTemplate copied = captor.getValue();
        BlocklyValidationResult validation = actualValidator.validateStored(
                copied.getBlocklyJson(), 2L, Map.of(targetParam.getId(), targetParam),
                BlocklyValidationMode.DRAFT);
        JsonNode extraState = validation.getBlocklyJson().findValue("extraState");
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).containsExactly("参数‘账户余额’引用场景与当前模板场景不一致");
        assertReboundParam(extraState, "1", "11",
                "balance", "账户余额", ParamType.NUMBER);
    }

    @Test
    void shouldKeepContentBindingWhenCopyingWithinSameScene() {
        initializeTemplateTableInfo();
        MsgSceneParam sourceParam = sceneParam(11L, 1L, "balance", "账户余额", ParamType.NUMBER);
        MsgTemplate source = sourceTemplate(1L, storedContent(1L, Map.of(11L, sourceParam),
                sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                        "balance-node", "1", "11", "balance", "账户余额", ParamType.NUMBER)));
        when(msgTemplateMapper.selectById(10L)).thenReturn(source);
        when(msgSceneMapper.selectById(1L)).thenReturn(enabledScene(1L));
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of(sourceParam));
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate copied = invocation.getArgument(0);
            copied.setId(20L);
            return 1;
        });

        actualTemplateService().copy(10L, copyRequest(1L, true));

        ArgumentCaptor<MsgTemplate> captor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).insert(captor.capture());
        JsonNode extraState = actualValidator.readNullable(captor.getValue().getBlocklyJson())
                .findValue("extraState");
        assertReboundParam(extraState, "1", "11", "balance", "账户余额", ParamType.NUMBER);
        verify(msgSceneParamMapper, org.mockito.Mockito.times(1)).selectList(any());
    }

    @Test
    void shouldKeepCopyContentFalseBehaviorUnchanged() {
        initializeTemplateTableInfo();
        MsgTemplate source = sourceTemplate(1L, "invalid content is intentionally ignored");
        when(msgTemplateMapper.selectById(10L)).thenReturn(source);
        when(msgSceneMapper.selectById(2L)).thenReturn(enabledScene(2L));
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate copied = invocation.getArgument(0);
            copied.setId(20L);
            return 1;
        });

        var result = actualTemplateService().copy(10L, copyRequest(2L, false));

        assertThat(result.getHasContent()).isFalse();
        ArgumentCaptor<MsgTemplate> captor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).insert(captor.capture());
        assertThat(captor.getValue().getBlocklyJson()).isNull();
        verify(msgSceneParamMapper, never()).selectList(any());
    }

    @Test
    void shouldCopyConflictingDraftAndRejectEnableUntilConflictIsResolved() {
        initializeTemplateTableInfo();
        MsgSceneParam sourceParam = sceneParam(11L, 1L, "param2", "2", ParamType.NUMBER);
        MsgTemplate source = sourceTemplate(1L, storedContent(1L, Map.of(11L, sourceParam),
                sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                        "param-2-node", "1", "11", "param2", "2", ParamType.NUMBER)));
        MsgTemplate[] copiedHolder = new MsgTemplate[1];
        when(msgTemplateMapper.selectById(10L)).thenReturn(source);
        when(msgTemplateMapper.selectById(20L)).thenAnswer(invocation -> copiedHolder[0]);
        when(msgSceneMapper.selectById(2L)).thenReturn(enabledScene(2L));
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.selectList(any()))
                .thenReturn(List.of(sourceParam), List.of(), List.of(), List.of());
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate copied = invocation.getArgument(0);
            copied.setId(20L);
            copiedHolder[0] = copied;
            return 1;
        });
        when(msgTemplateMapper.selectTemplateDetail(20L)).thenAnswer(invocation -> {
            MsgTemplate copied = copiedHolder[0];
            TemplateQueryRow row = new TemplateQueryRow();
            row.setId(copied.getId());
            row.setTemplateName(copied.getTemplateName());
            row.setSceneId(copied.getSceneId());
            row.setChannelType(copied.getChannelType());
            row.setBlocklyJson(copied.getBlocklyJson());
            row.setStatus(copied.getStatus());
            row.setUnitCount(0L);
            return row;
        });
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(20L)).thenReturn(List.of());

        var service = actualTemplateService();
        var result = service.copy(10L, copyRequest(2L, true));
        var detail = service.detail(20L);

        assertThat(result.getNewTemplateId()).isEqualTo(20L);
        assertThat(result.getHasContent()).isFalse();
        assertThat(detail.getBlocklyJson()).isNotNull();
        assertThat(detail.getHasContent()).isFalse();
        assertThat(detail.getContentStatusDesc()).isEqualTo("未编辑");
        assertThat(detail.getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
        JsonNode extraState = detail.getBlocklyJson().findValue("extraState");
        assertReboundParam(extraState, "1", "11", "param2", "2", ParamType.NUMBER);

        assertThatThrownBy(() -> service.toggle(20L))
                .isInstanceOf(BizException.class)
                .hasMessage("参数‘2’引用场景与当前模板场景不一致");

        verify(msgTemplateMapper).insert(any(MsgTemplate.class));
        verify(msgTemplateMapper, never()).updateById(any(MsgTemplate.class));
    }

    @Test
    void shouldReturnMultipleReadableParamErrorsWhenSavingHistoricalContent() {
        initializeTemplateTableInfo();
        MsgTemplate template = sourceTemplate(2L, null);
        when(msgTemplateMapper.selectById(10L)).thenReturn(template);
        when(msgSceneMapper.selectById(2L)).thenReturn(enabledScene(2L));
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());
        when(msgTemplateMapper.updateById(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate update = invocation.getArgument(0);
            template.setBlocklyJson(update.getBlocklyJson());
            return 1;
        });
        ObjectNode balanceBlock = sceneParamBlock(BlocklyBlockTypes.SCENE_PARAM_VALUE,
                "balance-node", "99", "11", "balance", "账户余额", ParamType.NUMBER);
        ObjectNode amountBlock = sceneParamBlock(BlocklyBlockTypes.LEGACY_SCENE_PARAM_REF,
                "amount-node", "99", "12", "amount", "消费金额", ParamType.NUMBER);
        TemplateContentSaveDTO request = new TemplateContentSaveDTO();
        request.setSchemaVersion(1);
        request.setWorkspace(linkedWorkspace(balanceBlock, amountBlock));

        var result = actualTemplateService().saveContent(10L, request);

        assertThat(result.getValid()).isFalse();
        assertThat(result.getErrors()).containsExactly(
                "参数‘账户余额’引用场景与当前模板场景不一致",
                "参数‘消费金额’引用场景与当前模板场景不一致");
        assertThat(result.getErrors()).allSatisfy(error ->
                assertThat(error).doesNotContain("scene_param_"));
        verify(msgTemplateMapper).updateById(any(MsgTemplate.class));
    }

    @Test
    void shouldRenderNestedArithmeticWithoutPrecisionLoss() {
        ObjectNode divide = binary(BlocklyBlockTypes.MATH_ARITHMETIC, "DIVIDE",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode modulo = binary(BlocklyBlockTypes.MATH_MODULO, null,
                "DIVIDEND", paramBlock(1L, "left", ParamType.NUMBER),
                "DIVISOR", paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode addition = binary(BlocklyBlockTypes.MATH_ARITHMETIC, "ADD",
                "A", divide, "B", modulo);

        BlocklyRenderResult result = render(addition, Map.of(
                "left", objectMapper.valueToTree(new BigDecimal("10.0")),
                "right", objectMapper.valueToTree(new BigDecimal("4"))
        ));

        assertThat(result.renderedContent()).isEqualTo("4.5");
        assertThat(result.usedParams()).containsExactly("left", "right");
    }

    @Test
    void shouldKeepExistingTextJoinAndFormatBlocksCompatible() {
        ObjectNode amount = unary(BlocklyBlockTypes.AMOUNT_FORMAT, "VALUE",
                paramBlock(1L, "left", ParamType.NUMBER));
        ObjectNode time = unary(BlocklyBlockTypes.TIME_FORMAT, "VALUE",
                paramBlock(4L, "sendTime", ParamType.TIME));
        ObjectNode join = objectMapper.createObjectNode();
        join.put("type", BlocklyBlockTypes.TEXT_JOIN);
        putInput(join, "ADD0", textBlock("金额："));
        putInput(join, "ADD1", amount);
        putInput(join, "ADD2", textBlock("，时间："));
        putInput(join, "ADD3", time);

        BlocklyRenderResult result = render(join, Map.of(
                "left", objectMapper.valueToTree(new BigDecimal("10")),
                "sendTime", objectMapper.valueToTree("2026-06-22 10:00:00")
        ));

        assertThat(result.renderedContent()).isEqualTo("金额：10.00，时间：2026-06-22 10:00:00");
        assertThat(result.usedParams()).containsExactly("left", "sendTime");
    }

    @Test
    void shouldSkipAmountFormatForEmptyOptionalParamAndContinueRendering() {
        ObjectNode amount = unary(BlocklyBlockTypes.AMOUNT_FORMAT, "VALUE",
                paramBlock(13L, "optionalAmount", ParamType.NUMBER));
        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", BlocklyBlockTypes.TEXT_JOIN);
        putInput(content, "ADD0", amount);
        putInput(content, "ADD1", textBlock("aaa"));

        for (Map<String, JsonNode> values : List.of(
                Map.<String, JsonNode>of(),
                Map.<String, JsonNode>of("optionalAmount", objectMapper.nullNode()),
                Map.<String, JsonNode>of("optionalAmount", objectMapper.valueToTree("")))) {
            assertThat(render(content, values).renderedContent()).isEqualTo("aaa");
        }
        assertThat(render(content, Map.of("optionalAmount", objectMapper.valueToTree(12)))
                .renderedContent()).isEqualTo("12.00aaa");
        assertThatThrownBy(() -> render(content,
                Map.of("optionalAmount", objectMapper.valueToTree("not-number"))))
                .isInstanceOf(BizException.class)
                .hasMessage("amount_format 的输入必须为数字");

        ObjectNode requiredAmount = unary(BlocklyBlockTypes.AMOUNT_FORMAT, "VALUE",
                paramBlock(1L, "left", ParamType.NUMBER));
        assertThatThrownBy(() -> render(requiredAmount, Map.of()))
                .isInstanceOf(BizException.class)
                .hasMessage("必填参数未提供：left");
    }

    @Test
    void shouldRenderLinkedNodeModeByTemplateNodeOrder() {
        ObjectNode hello = textBlock("你好，");
        hello.put("id", "text-1");
        ObjectNode param = paramBlock(10L, "prefix", ParamType.STRING);
        param.put("id", "param-1");
        ObjectNode suffix = objectMapper.createObjectNode();
        suffix.put("id", "join-1");
        suffix.put("type", BlocklyBlockTypes.TEXT_JOIN);
        suffix.putObject("fields").put("TEXT", "！");

        ObjectNode workspace = linkedWorkspace(hello, param, suffix);
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1, workspace, 1L,
                expressionParams, BlocklyValidationMode.DRAFT);

        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("prefix", objectMapper.valueToTree("南网")));

        assertThat(result.renderedContent()).isEqualTo("你好，南网！");
        assertThat(result.usedParams()).containsExactly("prefix");
    }

    @Test
    void shouldRejectMissingBlockInLinkedNodeOrder() {
        ObjectNode hello = textBlock("你好");
        hello.put("id", "text-1");
        ObjectNode workspace = linkedWorkspace(hello);
        ((ArrayNode) workspace.get("templateNodeOrder")).add("missing-1");

        assertThatThrownBy(() -> actualValidator.validateWorkspace(1, workspace, 1L,
                expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("模板节点不存在：missing-1");
    }

    @Test
    void shouldRenderLinkedMathCompareAndFormatNodes() {
        BlocklyValidationResult math = actualValidator.validateWorkspace(1,
                linkedWorkspace(linkedText("n1", "5"),
                        linkedOperation("op1", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD"),
                        linkedText("n2", "1")), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);
        assertThat(actualRenderer.render(math.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("6");

        BlocklyValidationResult amount = actualValidator.validateWorkspace(1,
                linkedWorkspace(linkedText("n3", "5"),
                        linkedDecimals("fmt1", BlocklyBlockTypes.AMOUNT_FORMAT, 1)), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);
        assertThat(actualRenderer.render(amount.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("5.0");

        BlocklyValidationResult compare = actualValidator.validateWorkspace(1,
                linkedWorkspace(linkedText("n4", "6"),
                        linkedOperation("cmp1", BlocklyBlockTypes.LOGIC_COMPARE, "GT"),
                        linkedText("n5", "4")), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);
        assertThat(actualRenderer.render(compare.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("true");
    }

    @Test
    void shouldSkipLinkedAmountFormatForEmptyOptionalParam() {
        ObjectNode amountParam = paramBlock(13L, "optionalAmount", ParamType.NUMBER);
        amountParam.put("id", "amount-param");
        ObjectNode amount = linkedDecimals("amount", BlocklyBlockTypes.AMOUNT_FORMAT, 2);
        ObjectNode suffix = linkedText("suffix", "aaa");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(amountParam, amount, suffix), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("aaa");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree(12)))
                .renderedContent()).isEqualTo("12.00aaa");
        assertThatThrownBy(() -> actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree("not-number"))))
                .isInstanceOf(BizException.class)
                .hasMessage("金额格式化节点相邻节点必须是数值");
    }

    @Test
    void shouldRenderGraphMathAdditionThenTextJoinSuffix() {
        ObjectNode left = paramBlock(1L, "left", ParamType.NUMBER);
        left.put("id", "left-param");
        ObjectNode right = paramBlock(2L, "right", ParamType.NUMBER);
        right.put("id", "right-param");
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        ObjectNode suffix = textJoinBlock("元");
        suffix.put("id", "suffix");

        ObjectNode workspace = graphWorkspace("add", left, right, addition, suffix);
        putMathExpression(workspace, "add", "left-param", "right-param");
        putLink(workspace, "add", "suffix", "input");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of(
                        "left", objectMapper.valueToTree(5),
                        "right", objectMapper.valueToTree(5)
                ));

        assertThat(result.renderedContent()).isEqualTo("10元");
    }

    @Test
    void shouldRenderMathInputPrefixThenExpressionResultAndSuffix() {
        ObjectNode prefix = textJoinBlock("订单金额：");
        prefix.put("id", "prefix");
        ObjectNode amount = paramBlock(1L, "left", ParamType.NUMBER);
        amount.put("id", "amount");
        ObjectNode five = mathNumber("five", 5);
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "MINUS");
        ObjectNode suffix = textJoinBlock("元");
        suffix.put("id", "suffix");

        ObjectNode workspace = graphWorkspace("suffix", prefix, amount, five, addition, suffix);
        putLink(workspace, "prefix", "add", "input");
        putLink(workspace, "amount", "add", "leftValue");
        putLink(workspace, "five", "add", "rightValue");
        putLink(workspace, "add", "suffix", "input");
        putMathExpression(workspace, "add", "amount", "five");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);
        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("left", objectMapper.valueToTree(10)));

        assertThat(result.renderedContent()).isEqualTo("订单金额：5元");
        assertThat(result.usedParams()).containsExactly("left");
    }

    @Test
    void shouldRenderMathResultAndSuffixWithoutPreviousContent() {
        ObjectNode passengerCount = paramBlock(1L, "left", ParamType.NUMBER);
        passengerCount.put("id", "passenger-count");
        ObjectNode five = mathNumber("five", 5);
        ObjectNode subtraction = linkedOperation("subtract", BlocklyBlockTypes.MATH_ARITHMETIC, "MINUS");
        ObjectNode suffix = linkedText("suffix", "元");
        ObjectNode workspace = graphWorkspace("suffix", passengerCount, five, subtraction, suffix);
        putLink(workspace, "passenger-count", "subtract", "leftValue");
        putLink(workspace, "five", "subtract", "rightValue");
        putLink(workspace, "subtract", "suffix", "input");
        putMathExpression(workspace, "subtract", "passenger-count", "five");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);
        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("left", objectMapper.valueToTree(10)));

        assertThat(result.renderedContent()).isEqualTo("5元");
    }

    @Test
    void shouldKeepCompletePreviousContentWhenMathNodeIsBranchTailPredecessor() {
        ObjectNode title = linkedText("title", "标题-");
        ObjectNode merchant = linkedText("merchant", "商户-");
        ObjectNode prefix = linkedText("prefix", "共");
        ObjectNode passengerCount = paramBlock(1L, "left", ParamType.NUMBER);
        passengerCount.put("id", "passenger-count");
        ObjectNode five = mathNumber("five", 5);
        ObjectNode subtraction = linkedOperation("subtract", BlocklyBlockTypes.MATH_ARITHMETIC, "MINUS");
        ObjectNode suffix = linkedText("suffix", "元");
        ObjectNode thenText = linkedText("then", "另一分支");
        ObjectNode condition = paramBlock(12L, "isPark", ParamType.BOOLEAN);
        condition.put("id", "condition");
        ObjectNode controlsIf = objectMapper.createObjectNode();
        controlsIf.put("id", "if1");
        controlsIf.put("type", BlocklyBlockTypes.CONTROLS_IF);

        ObjectNode workspace = graphWorkspace("if1", title, merchant, prefix, passengerCount,
                five, subtraction, suffix, thenText, condition, controlsIf);
        putLink(workspace, "title", "merchant", "input");
        putLink(workspace, "merchant", "prefix", "input");
        putLink(workspace, "prefix", "subtract", "input");
        putLink(workspace, "passenger-count", "subtract", "leftValue");
        putLink(workspace, "five", "subtract", "rightValue");
        putLink(workspace, "subtract", "suffix", "input");
        putMathExpression(workspace, "subtract", "passenger-count", "five");
        workspace.putObject("templateBranches")
                .putObject("if1")
                .put("conditionBlockId", "condition")
                .put("thenBlockId", "then")
                .put("elseBlockId", "suffix");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);
        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of(
                        "left", objectMapper.valueToTree(10),
                        "isPark", objectMapper.valueToTree(false)));

        assertThat(result.renderedContent()).isEqualTo("标题-商户-共5元");
        assertThat(result.renderedContent()).doesNotContain("1055");
    }

    @Test
    void shouldKeepGraphMathSubtractionOrder() {
        ObjectNode left = linkedText("left-number", "10");
        ObjectNode right = linkedText("right-number", "3");
        ObjectNode minus = linkedOperation("minus", BlocklyBlockTypes.MATH_ARITHMETIC, "MINUS");
        ObjectNode workspace = graphWorkspace("minus", left, right, minus);
        putMathExpression(workspace, "minus", "left-number", "right-number");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("7");
    }

    @Test
    void shouldRejectGraphDivisionByZero() {
        ObjectNode left = linkedText("left-number", "10");
        ObjectNode zero = linkedText("zero-number", "0");
        ObjectNode divide = linkedOperation("divide", BlocklyBlockTypes.MATH_ARITHMETIC, "DIVIDE");
        ObjectNode workspace = graphWorkspace("divide", left, zero, divide);
        putMathExpression(workspace, "divide", "left-number", "zero-number");

        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 divide：除数不能为 0");
    }

    @Test
    void shouldRenderGraphModulo() {
        ObjectNode left = linkedText("left-number", "10");
        ObjectNode right = linkedText("right-number", "3");
        ObjectNode modulo = linkedOperation("modulo", BlocklyBlockTypes.MATH_MODULO, null);
        ObjectNode workspace = graphWorkspace("modulo", left, right, modulo);
        putMathExpression(workspace, "modulo", "left-number", "right-number");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("1");
    }

    @Test
    void shouldRejectGraphModuloByZeroWithReadableMessage() {
        ObjectNode left = linkedText("left-number", "10");
        ObjectNode zero = linkedText("zero-number", "0");
        ObjectNode modulo = linkedOperation("modulo", BlocklyBlockTypes.MATH_MODULO, null);
        ObjectNode workspace = graphWorkspace("modulo", left, zero, modulo);
        putMathExpression(workspace, "modulo", "left-number", "zero-number");

        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 modulo：取余除数不能为 0");
    }

    @Test
    void shouldRecursivelyCalculateMathNumbersAndJoinResultAsText() {
        ObjectNode amount = paramBlock(1L, "left", ParamType.NUMBER);
        amount.put("id", "amount");
        ObjectNode twenty = mathNumber("twenty", 20);
        ObjectNode two = mathNumber("two", "2");
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        ObjectNode multiply = linkedOperation("multiply", BlocklyBlockTypes.MATH_ARITHMETIC, "MULTIPLY");
        ObjectNode prefix = linkedText("prefix", "余额：");
        ObjectNode content = objectMapper.createObjectNode();
        content.put("id", "content");
        content.put("type", BlocklyBlockTypes.MESSAGE_CONTENT);

        ObjectNode workspace = graphWorkspace("content", amount, twenty, two, addition, multiply, prefix, content);
        workspace.remove("templateNodeOrder");
        putMathExpression(workspace, "add", "amount", "twenty");
        putMathExpression(workspace, "multiply", "add", "two");
        putLink(workspace, "prefix", "content", "input");
        putLink(workspace, "multiply", "content", "input");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);
        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("left", objectMapper.valueToTree(100)));

        assertThat(result.renderedContent()).isEqualTo("余额：240");
    }

    @Test
    void shouldCalculateMathNumberConstantsAndUseUnifiedDivisionPrecision() {
        ObjectNode ten = mathNumber("ten", 10);
        ObjectNode five = mathNumber("five", "5");
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        ObjectNode addWorkspace = graphWorkspace("add", ten, five, addition);
        putMathExpression(addWorkspace, "add", "ten", "five");

        BlocklyValidationResult addValidation = actualValidator.validateWorkspace(1,
                addWorkspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);
        assertThat(actualRenderer.render(addValidation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("15");

        ObjectNode four = mathNumber("four", 4);
        ObjectNode division = linkedOperation("division", BlocklyBlockTypes.MATH_ARITHMETIC, "DIVIDE");
        ObjectNode divideWorkspace = graphWorkspace("division", ten, four, division);
        putMathExpression(divideWorkspace, "division", "ten", "four");
        BlocklyValidationResult divideValidation = actualValidator.validateWorkspace(1,
                divideWorkspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(divideValidation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("2.5");
    }

    @Test
    void shouldRejectInvalidGraphMathStructureWithNodeId() {
        ObjectNode invalidNumber = mathNumber("bad-number", "not-number");
        ObjectNode one = mathNumber("one", 1);
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        ObjectNode invalidNumberWorkspace = graphWorkspace("add", invalidNumber, one, addition);
        putMathExpression(invalidNumberWorkspace, "add", "bad-number", "one");
        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                invalidNumberWorkspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 bad-number：fields.NUM 不是有效数字");

        ObjectNode missingOperandWorkspace = graphWorkspace("add", one, addition);
        putMathExpression(missingOperandWorkspace, "add", "one", null);
        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                missingOperandWorkspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 add：缺少右操作数");

        ObjectNode cycleWorkspace = graphWorkspace("add", one, addition);
        putMathExpression(cycleWorkspace, "add", "add", "one");
        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                cycleWorkspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 add：表达式存在循环引用");

        ObjectNode unknown = linkedOperation("unknown", BlocklyBlockTypes.MATH_ARITHMETIC, "POWER");
        ObjectNode unknownWorkspace = graphWorkspace("unknown", one, unknown);
        putMathExpression(unknownWorkspace, "unknown", "one", "one");
        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                unknownWorkspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 unknown：未知 operation：POWER");
    }

    @Test
    void shouldReportMathNodeIdWhenNumberParameterIsMissing() {
        ObjectNode amount = paramBlock(1L, "left", ParamType.NUMBER);
        amount.put("id", "amount");
        ObjectNode one = mathNumber("one", 1);
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        ObjectNode workspace = graphWorkspace("add", amount, one, addition);
        putMathExpression(workspace, "add", "amount", "one");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThatThrownBy(() -> actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 add：左操作数求值失败：必填参数未提供：left");
    }

    @Test
    void shouldRejectGraphMathExpressionOverDepthLimit() {
        ObjectNode[] blocks = new ObjectNode[101];
        blocks[0] = mathNumber("number", 1);
        for (int index = 1; index <= 100; index++) {
            blocks[index] = linkedOperation("op" + index,
                    BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        }
        ObjectNode workspace = graphWorkspace("op100", blocks);
        for (int index = 1; index <= 100; index++) {
            putMathExpression(workspace, "op" + index,
                    index == 1 ? "number" : "op" + (index - 1), "number");
        }

        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("节点 op100：表达式递归层级超过 100");
    }

    @Test
    void shouldRenderGraphMathResultAsAmountFormatInput() {
        ObjectNode left = linkedText("left-number", "5");
        ObjectNode right = linkedText("right-number", "5");
        ObjectNode addition = linkedOperation("add", BlocklyBlockTypes.MATH_ARITHMETIC, "ADD");
        ObjectNode amount = linkedDecimals("amount", BlocklyBlockTypes.AMOUNT_FORMAT, 2);
        ObjectNode workspace = graphWorkspace("amount", left, right, addition, amount);
        putMathExpression(workspace, "add", "left-number", "right-number");
        putLink(workspace, "add", "amount", "input");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("10.00");
    }

    @Test
    void shouldRenderGraphAmountFormatWithPrefixedParamInputAsTextSegment() {
        ObjectNode prefix = textJoinBlock("本园区就餐消费成功，共消费了");
        prefix.put("id", "prefix");
        ObjectNode amountParam = paramBlock(13L, "optionalAmount", ParamType.NUMBER);
        amountParam.put("id", "amount-param");
        ObjectNode amount = linkedDecimals("amount", BlocklyBlockTypes.AMOUNT_FORMAT, 2);
        ObjectNode suffix = textJoinBlock("元");
        suffix.put("id", "suffix");
        ObjectNode workspace = graphWorkspace("suffix", prefix, amountParam, amount, suffix);
        putLink(workspace, "prefix", "amount-param", "input");
        putLink(workspace, "amount-param", "amount", "input");
        putLink(workspace, "amount", "suffix", "input");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree(55))).renderedContent())
                .isEqualTo("本园区就餐消费成功，共消费了55.00元");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent())
                .isEqualTo("本园区就餐消费成功，共消费了元");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.nullNode())).renderedContent())
                .isEqualTo("本园区就餐消费成功，共消费了元");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree(""))).renderedContent())
                .isEqualTo("本园区就餐消费成功，共消费了元");
        assertThatThrownBy(() -> actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree("not-number"))))
                .isInstanceOf(BizException.class)
                .hasMessage("amount_format 的输入必须为数字");
    }

    @Test
    void shouldRenderGraphControlsIfByBranchesAndComparePorts() {
        ObjectNode amount = paramBlock(13L, "optionalAmount", ParamType.NUMBER);
        amount.put("id", "amount");
        ObjectNode threshold = linkedText("threshold", "2");
        ObjectNode compare = linkedOperation("compare", BlocklyBlockTypes.LOGIC_COMPARE, "EQ");
        ObjectNode thenText = linkedText("then", "11");
        ObjectNode elseText = linkedText("else", "22");
        ObjectNode controlsIf = objectMapper.createObjectNode();
        controlsIf.put("id", "if1");
        controlsIf.put("type", BlocklyBlockTypes.CONTROLS_IF);

        ObjectNode workspace = graphWorkspace("threshold", amount, threshold, compare, thenText, elseText, controlsIf);
        putLink(workspace, "amount", "compare", "left");
        putLink(workspace, "threshold", "compare", "right");
        putLinkWithoutTargetPort(workspace, "if1", "compare");
        putLink(workspace, "if1", "compare", "condition");
        putLink(workspace, "if1", "then", "then", "input");
        putLink(workspace, "if1", "else", "else", "input");
        workspace.putObject("templateBranches")
                .putObject("if1")
                .put("conditionBlockId", "compare")
                .put("thenBlockId", "then")
                .put("elseBlockId", "else");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree(2))).renderedContent())
                .isEqualTo("11");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree(1))).renderedContent())
                .isEqualTo("22");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of()).renderedContent()).isEqualTo("22");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.nullNode())).renderedContent())
                .isEqualTo("22");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree(""))).renderedContent())
                .isEqualTo("22");
        assertThatThrownBy(() -> actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("optionalAmount", objectMapper.valueToTree("not-number"))))
                .isInstanceOf(BizException.class)
                .hasMessage("logic_compare 的输入必须为数字");
    }

    @Test
    void shouldRenderGraphControlsIfWithDirectBooleanParam() {
        ObjectNode isPark = paramBlock(12L, "isPark", ParamType.BOOLEAN);
        isPark.put("id", "is-park");
        ObjectNode thenText = linkedText("then", "正确分支");
        ObjectNode elseText = linkedText("else", "错误分支");
        ObjectNode controlsIf = objectMapper.createObjectNode();
        controlsIf.put("id", "if1");
        controlsIf.put("type", BlocklyBlockTypes.CONTROLS_IF);

        ObjectNode workspace = graphWorkspace("if1", isPark, thenText, elseText, controlsIf);
        workspace.putObject("templateBranches")
                .putObject("if1")
                .put("conditionBlockId", "is-park")
                .put("thenBlockId", "then")
                .put("elseBlockId", "else");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L, expressionParams,
                Map.of("isPark", objectMapper.valueToTree(true))).renderedContent())
                .isEqualTo("正确分支");
        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L, expressionParams,
                Map.of("isPark", objectMapper.valueToTree(false))).renderedContent())
                .isEqualTo("错误分支");
    }

    @Test
    void shouldRenderGraphForEachBodyFromTailLoopItemFieldChain() {
        ObjectNode wallets = paramBlock(11L, "wallets", ParamType.OBJECT_ARRAY);
        wallets.put("id", "wallets");
        ObjectNode forEach = objectMapper.createObjectNode();
        forEach.put("id", "loop");
        forEach.put("type", BlocklyBlockTypes.CONTROLS_FOR_EACH);
        ObjectNode prefix = linkedText("prefix", "支付");
        ObjectNode paid = loopItemField("paid", ParamType.NUMBER);
        paid.put("id", "paid");
        ObjectNode middle = linkedText("middle", "元，余额");
        ObjectNode balance = loopItemField("balance", ParamType.NUMBER);
        balance.put("id", "balance");
        ObjectNode suffix = linkedText("suffix", "元");

        ObjectNode workspace = graphWorkspace("loop", wallets, forEach, prefix, paid, middle, balance, suffix);
        workspace.putObject("templateLoops")
                .putObject("loop")
                .put("collectionBlockId", "wallets")
                .put("bodyBlockId", "suffix");
        putLink(workspace, "prefix", "paid", "input");
        putLink(workspace, "paid", "middle", "input");
        putLink(workspace, "middle", "balance", "input");
        putLink(workspace, "balance", "suffix", "input");

        ObjectNode wallet = objectMapper.createObjectNode();
        wallet.put("name", "通用账户");
        wallet.put("paid", 10.00);
        wallet.put("balance", 230.00);
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);

        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("wallets", objectMapper.valueToTree(List.of(wallet))));

        assertThat(result.renderedContent()).isEqualTo("支付10元，余额230元");
        assertThat(result.usedParams()).containsExactly("wallets");
    }

    @Test
    void shouldRenderGraphForEachWithPrefixInputAndCollectionLink() {
        ObjectNode prefixText = linkedText("prefix-text", "其中");
        ObjectNode wallets = paramBlock(11L, "wallets", ParamType.OBJECT_ARRAY);
        wallets.put("id", "wallets");
        ObjectNode forEach = objectMapper.createObjectNode();
        forEach.put("id", "loop");
        forEach.put("type", BlocklyBlockTypes.CONTROLS_FOR_EACH);
        ObjectNode name = loopItemField("name", ParamType.STRING);
        name.put("id", "name");
        ObjectNode paidPrefix = linkedText("paid-prefix", "支付");
        ObjectNode paid = loopItemField("paid", ParamType.NUMBER);
        paid.put("id", "paid");
        ObjectNode middle = linkedText("middle", "元，余额");
        ObjectNode balance = loopItemField("balance", ParamType.NUMBER);
        balance.put("id", "balance");
        ObjectNode suffix = linkedText("suffix", "元");

        ObjectNode workspace = graphWorkspace("prefix-text", prefixText, forEach, wallets,
                name, paidPrefix, paid, middle, balance, suffix);
        workspace.putObject("templateLoops")
                .putObject("loop")
                .put("bodyBlockId", "suffix");
        putLink(workspace, "prefix-text", "loop", "input");
        putLink(workspace, "wallets", "loop", "collection");
        putLink(workspace, "name", "paid-prefix", "input");
        putLink(workspace, "paid-prefix", "paid", "input");
        putLink(workspace, "paid", "middle", "input");
        putLink(workspace, "middle", "balance", "input");
        putLink(workspace, "balance", "suffix", "input");

        ObjectNode wallet = objectMapper.createObjectNode();
        wallet.put("name", "通用账户");
        wallet.put("paid", 10.00);
        wallet.put("balance", 230.00);
        ObjectNode wallet2 = objectMapper.createObjectNode();
        wallet2.put("name", "通用账户1");
        wallet2.put("paid", 11.00);
        wallet2.put("balance", 231.00);

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                workspace, 1L, expressionParams, BlocklyValidationMode.DRAFT);
        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("wallets", objectMapper.valueToTree(List.of(wallet, wallet2))));

        assertThat(result.renderedContent())
                .isEqualTo("其中通用账户支付10元，余额230元通用账户1支付11元，余额231元");
        assertThat(result.usedParams()).containsExactly("wallets");
    }

    @Test
    void shouldRenderLinkedTimeFormatWithRawPreviewValue() {
        ObjectNode sendTime = paramBlock(4L, "sendTime", ParamType.TIME);
        sendTime.put("id", "time-param");
        ObjectNode formatter = linkedTimeFormat("time-format", "yyyy-MM-dd HH:mm:ss");

        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(sendTime, formatter), 1L, expressionParams, BlocklyValidationMode.DRAFT);

        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("sendTime", objectMapper.valueToTree("2026-11-11 11:11:11")));

        assertThat(result.renderedContent()).isEqualTo("2026-11-11 11:11:11");
        assertThat(result.usedParams()).containsExactly("sendTime");
    }

    @Test
    void shouldRenderEmptyLinkedTimeAsEmptyAndContinueFollowingNodes() {
        ObjectNode sendTime = paramBlock(4L, "sendTime", ParamType.TIME);
        sendTime.put("id", "time-param");
        ObjectNode formatter = linkedTimeFormat("time-format", "yyyy-MM-dd HH:mm:ss");
        ObjectNode suffix = linkedText("suffix", "后续节点");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(sendTime, formatter, suffix), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);

        for (JsonNode emptyValue : List.of(
                objectMapper.nullNode(),
                objectMapper.getNodeFactory().textNode(""),
                objectMapper.getNodeFactory().textNode(" \t\r\n"))) {
            BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                    expressionParams, Map.of("sendTime", emptyValue));

            assertThat(result.renderedContent()).isEqualTo("后续节点");
            assertThat(result.usedParams()).containsExactly("sendTime");
        }
    }

    @Test
    void shouldRenderLinkedTimeWithMilliseconds() {
        ObjectNode sendTime = paramBlock(4L, "sendTime", ParamType.TIME);
        sendTime.put("id", "time-param");
        ObjectNode formatter = linkedTimeFormat("time-format", "yyyy-MM-dd HH:mm:ss.SSS");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(sendTime, formatter), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);

        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("sendTime",
                        objectMapper.valueToTree("2011-12-13 11:12:13.123")));

        assertThat(result.renderedContent()).isEqualTo("2011-12-13 11:12:13.123");
    }

    @Test
    void shouldRenderLinkedTimeWithCustomPatternForAllSupportedInputs() {
        ObjectNode sendTime = paramBlock(4L, "sendTime", ParamType.TIME);
        sendTime.put("id", "time-param");
        ObjectNode formatter = linkedTimeFormat("time-format", "yyyy年MM月dd日");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(sendTime, formatter), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);

        for (JsonNode value : List.<JsonNode>of(
                objectMapper.valueToTree("2026-11-12 11:12:13.123"),
                objectMapper.valueToTree("2026-11-12T03:12:13Z"),
                objectMapper.valueToTree(1794453133000L))) {
            BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                    expressionParams, Map.of("sendTime", value));
            assertThat(result.renderedContent()).isEqualTo("2026年11月12日");
        }
    }

    @Test
    void shouldUseLinkedTimeFormatOnlyForOutput() {
        ObjectNode sendTime = paramBlock(4L, "sendTime", ParamType.TIME);
        sendTime.put("id", "time-param");
        ObjectNode formatter = linkedTimeFormat("time-format", "yyyy-MM-dd");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(sendTime, formatter), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);

        BlocklyRenderResult result = actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("sendTime",
                        objectMapper.valueToTree("2011-12-13 11:12:13.123")));

        assertThat(result.renderedContent()).isEqualTo("2011-12-13");
    }

    @Test
    void shouldDistinguishLinkedTimeValueAndFormatErrors() {
        ObjectNode sendDate = paramBlock(4L, "sendTime", ParamType.TIME);
        sendDate.put("id", "time-param");
        ObjectNode formatter = linkedTimeFormat("time-format", "yyyy-MM-dd HH:mm:ss");
        BlocklyValidationResult validation = actualValidator.validateWorkspace(1,
                linkedWorkspace(sendDate, formatter), 1L, expressionParams, BlocklyValidationMode.DRAFT);

        assertThat(actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("sendTime", objectMapper.valueToTree("2026-11-11")))
                .renderedContent()).isEqualTo("2026-11-11 00:00:00");

        ObjectNode invalidFormatter = linkedTimeFormat("bad-format", "yyyy-MM-dd HH:mm:ss '");
        assertThatThrownBy(() -> actualValidator.validateWorkspace(1,
                linkedWorkspace(sendDate, invalidFormatter), 1L, expressionParams, BlocklyValidationMode.DRAFT))
                .isInstanceOf(BizException.class)
                .hasMessage("时间格式模板不合法：yyyy-MM-dd HH:mm:ss '");

        assertThatThrownBy(() -> actualRenderer.render(validation.getBlocklyJson(), 1L,
                expressionParams, Map.of("sendTime", objectMapper.valueToTree("not-time"))))
                .isInstanceOf(BizException.class)
                .hasMessage("时间值格式不合法，应为 yyyy-MM-dd HH:mm:ss");
    }

    @Test
    void shouldRejectDivisionAndModuloByZero() {
        ObjectNode division = binary(BlocklyBlockTypes.MATH_ARITHMETIC, "DIVIDE",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", paramBlock(3L, "zero", ParamType.NUMBER));
        ObjectNode modulo = binary(BlocklyBlockTypes.MATH_MODULO, null,
                "DIVIDEND", paramBlock(1L, "left", ParamType.NUMBER),
                "DIVISOR", paramBlock(3L, "zero", ParamType.NUMBER));
        Map<String, JsonNode> values = Map.of(
                "left", objectMapper.valueToTree(10),
                "zero", objectMapper.valueToTree(0)
        );

        assertThatThrownBy(() -> render(division, values))
                .isInstanceOf(BizException.class)
                .hasMessage("math_arithmetic 的除数不能为 0");
        assertThatThrownBy(() -> render(modulo, values))
                .isInstanceOf(BizException.class)
                .hasMessage("math_modulo 的除数不能为 0");
    }

    @Test
    void shouldRejectInvalidExpressionTypeAndOperatorDuringValidation() {
        ObjectNode invalidType = binary(BlocklyBlockTypes.MATH_ARITHMETIC, "ADD",
                "A", textBlock("1"),
                "B", paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode invalidOperator = binary(BlocklyBlockTypes.LOGIC_COMPARE, "MATCH",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", paramBlock(2L, "right", ParamType.NUMBER));

        assertThatThrownBy(() -> validate(invalidType))
                .isInstanceOf(BizException.class)
                .hasMessage("math_arithmetic 的输入必须为 数字");
        assertThatThrownBy(() -> validate(invalidOperator))
                .isInstanceOf(BizException.class)
                .hasMessage("logic_compare 的操作符 OP 不支持");
    }

    @Test
    void shouldEvaluateBooleanAndEscapedLikeExpressions() throws Exception {
        ObjectNode greater = binary(BlocklyBlockTypes.LOGIC_COMPARE, "GT",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode divideByZero = binary(BlocklyBlockTypes.MATH_ARITHMETIC, "DIVIDE",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", paramBlock(3L, "zero", ParamType.NUMBER));
        ObjectNode unreachableCompare = binary(BlocklyBlockTypes.LOGIC_COMPARE, "GT",
                "A", divideByZero,
                "B", paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode shortCircuitOr = binary(BlocklyBlockTypes.LOGIC_OPERATION, "OR",
                "A", greater, "B", unreachableCompare);
        ObjectNode negate = unary(BlocklyBlockTypes.LOGIC_NEGATE, "BOOL", greater);
        ObjectNode contains = binary(BlocklyBlockTypes.STRING_CONTAINS, null,
                "TEXT", textBlock("message-center"),
                "SUBSTRING", textBlock("center"));
        ObjectNode like = binary(BlocklyBlockTypes.STRING_LIKE, null,
                "TEXT", textBlock("a.zz[x]"),
                "PATTERN", textBlock("a.%[x]"));
        ObjectNode regexNotExecuted = binary(BlocklyBlockTypes.STRING_LIKE, null,
                "TEXT", textBlock("abc"),
                "PATTERN", textBlock("a.*"));
        Map<String, JsonNode> values = Map.of(
                "left", objectMapper.valueToTree(10),
                "right", objectMapper.valueToTree(4),
                "zero", objectMapper.valueToTree(0)
        );

        assertThat(evaluateExpression(shortCircuitOr, values)).isEqualTo(true);
        assertThat(evaluateExpression(negate, values)).isEqualTo(false);
        assertThat(evaluateExpression(contains, values)).isEqualTo(true);
        assertThat(evaluateExpression(like, values)).isEqualTo(true);
        assertThat(evaluateExpression(regexNotExecuted, values)).isEqualTo(false);
    }

    @Test
    void shouldAllowNumericTextInLogicCompare() throws Exception {
        ObjectNode greater = binary(BlocklyBlockTypes.LOGIC_COMPARE, "GT",
                "A", textBlock("10.5"),
                "B", textJoinBlock("2"));
        ObjectNode equals = binary(BlocklyBlockTypes.LOGIC_COMPARE, "EQ",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", textBlock("10"));

        assertThat(evaluateExpression(greater, Map.of())).isEqualTo(true);
        assertThat(evaluateExpression(equals, Map.of("left", objectMapper.valueToTree(10)))).isEqualTo(true);
    }

    @Test
    void shouldRenderElseBranchAndContinueWhenOptionalCompareParamIsEmpty() {
        ObjectNode condition = compare("GT",
                paramBlock(13L, "optionalAmount", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode conditional = controlsIf(condition, textBlock("正确分支"));
        addElse(conditional, textBlock("错误分支"));
        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", BlocklyBlockTypes.TEXT_JOIN);
        putInput(content, "ADD0", conditional);
        putInput(content, "ADD1", textBlock("后续节点"));

        for (Map<String, JsonNode> values : List.of(
                Map.<String, JsonNode>of("right", objectMapper.valueToTree(4)),
                Map.<String, JsonNode>of("optionalAmount", objectMapper.nullNode(),
                        "right", objectMapper.valueToTree(4)),
                Map.<String, JsonNode>of("optionalAmount", objectMapper.valueToTree(""),
                        "right", objectMapper.valueToTree(4)))) {
            assertThat(render(content, values).renderedContent())
                    .isEqualTo("错误分支后续节点");
        }
    }

    @Test
    void shouldCompareLegalOptionalNumberAndTimeValues() {
        ObjectNode numberCondition = compare("GT",
                paramBlock(13L, "optionalAmount", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode numberConditional = controlsIf(numberCondition, textBlock("数字正确"));
        addElse(numberConditional, textBlock("数字错误"));
        assertThat(render(numberConditional, Map.of(
                "optionalAmount", objectMapper.valueToTree(5),
                "right", objectMapper.valueToTree(4))).renderedContent()).isEqualTo("数字正确");

        ObjectNode timeCondition = compare("GT",
                paramBlock(14L, "optionalTime", ParamType.TIME),
                paramBlock(4L, "sendTime", ParamType.TIME));
        ObjectNode timeConditional = controlsIf(timeCondition, textBlock("时间正确"));
        addElse(timeConditional, textBlock("时间错误"));
        assertThat(render(timeConditional, Map.of(
                "optionalTime", objectMapper.valueToTree("2026-08-05 12:00:00"),
                "sendTime", objectMapper.valueToTree("2026-08-05 11:00:00")))
                .renderedContent()).isEqualTo("时间正确");
    }

    @Test
    void shouldRejectNonEmptyInvalidCompareParamTypes() {
        ObjectNode numberCompare = compare("GT",
                paramBlock(13L, "optionalAmount", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        assertThatThrownBy(() -> evaluateExpression(numberCompare, Map.of(
                "optionalAmount", objectMapper.valueToTree("not-number"),
                "right", objectMapper.valueToTree(4))))
                .isInstanceOf(BizException.class)
                .hasMessage("logic_compare 的输入必须为数字");

        ObjectNode timeCompare = compare("GT",
                paramBlock(14L, "optionalTime", ParamType.TIME),
                paramBlock(4L, "sendTime", ParamType.TIME));
        assertThatThrownBy(() -> evaluateExpression(timeCompare, Map.of(
                "optionalTime", objectMapper.valueToTree(123),
                "sendTime", objectMapper.valueToTree("2026-08-05 11:00:00"))))
                .isInstanceOf(BizException.class)
                .hasMessage("logic_compare 的输入必须为时间");
    }

    @Test
    void shouldKeepRequiredCompareParamValidation() {
        ObjectNode condition = compare("GT",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));

        assertThatThrownBy(() -> evaluateExpression(condition,
                Map.of("right", objectMapper.valueToTree(4))))
                .isInstanceOf(BizException.class)
                .hasMessage("必填参数未提供：left");
        assertThatThrownBy(() -> evaluateExpression(condition, Map.of(
                "left", objectMapper.nullNode(),
                "right", objectMapper.valueToTree(4))))
                .isInstanceOf(BizException.class)
                .hasMessage("必填参数值为空：left");
    }

    @Test
    void shouldShortCircuitControlsIfAndTrackOnlyExecutedBranchParams() {
        ObjectNode trueCondition = compare("GT",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode divideByZero = binary(BlocklyBlockTypes.MATH_ARITHMETIC, "DIVIDE",
                "A", paramBlock(1L, "left", ParamType.NUMBER),
                "B", paramBlock(3L, "zero", ParamType.NUMBER));
        ObjectNode unreachableCondition = compare("GT", divideByZero,
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode conditional = controlsIf(trueCondition,
                paramBlock(5L, "branch0", ParamType.STRING));
        addElseIf(conditional, 1, unreachableCondition,
                paramBlock(6L, "branch1", ParamType.STRING));
        addElse(conditional, paramBlock(7L, "elseBranch", ParamType.STRING));

        BlocklyRenderResult result = render(conditional, Map.of(
                "left", objectMapper.valueToTree(10),
                "right", objectMapper.valueToTree(4),
                "zero", objectMapper.valueToTree(0),
                "branch0", objectMapper.valueToTree("DO0"),
                "branch1", objectMapper.valueToTree("DO1"),
                "elseBranch", objectMapper.valueToTree("ELSE")
        ));

        assertThat(result.renderedContent()).isEqualTo("DO0");
        assertThat(result.usedParams()).containsExactly("left", "right", "branch0");
        assertThat(result.warnings()).hasSize(3);
    }

    @Test
    void shouldRenderElseIfElseAndEmptyControlsIfResults() {
        ObjectNode falseCondition = compare("LT",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode trueCondition = compare("GT",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode elseIf = controlsIf(falseCondition, textBlock("DO0"));
        addElseIf(elseIf, 1, trueCondition, textBlock("DO1"));
        addElse(elseIf, textBlock("ELSE"));

        ObjectNode elseOnly = controlsIf(falseCondition, textBlock("DO0"));
        addElse(elseOnly, textBlock("ELSE"));

        ObjectNode noElse = controlsIf(falseCondition, textBlock("DO0"));

        Map<String, JsonNode> values = Map.of(
                "left", objectMapper.valueToTree(10),
                "right", objectMapper.valueToTree(4)
        );
        assertThat(render(elseIf, values).renderedContent()).isEqualTo("DO1");
        assertThat(render(elseOnly, values).renderedContent()).isEqualTo("ELSE");
        assertThat(render(noElse, values).renderedContent()).isEmpty();
    }

    @Test
    void shouldRenderRequiredBooleanConditionAndRejectStringValue() throws Exception {
        ObjectNode conditional = controlsIf(
                paramBlock(12L, "isPark", ParamType.BOOLEAN), textBlock("正确分支"));
        addElse(conditional, textBlock("错误分支"));

        assertThat(render(conditional,
                Map.of("isPark", objectMapper.valueToTree(true))).renderedContent())
                .isEqualTo("正确分支");
        assertThat(render(conditional,
                Map.of("isPark", objectMapper.valueToTree(false))).renderedContent())
                .isEqualTo("错误分支");
        assertThatThrownBy(() -> render(conditional,
                Map.of("isPark", objectMapper.valueToTree("true"))))
                .isInstanceOf(BizException.class)
                .hasMessage("参数isPark必须是JSON布尔值");
        assertThatThrownBy(() -> render(conditional, Map.of()))
                .isInstanceOf(BizException.class)
                .hasMessage("必填参数未提供：isPark");
        Map<String, JsonNode> nullValue = new LinkedHashMap<>();
        nullValue.put("isPark", objectMapper.nullNode());
        assertThatThrownBy(() -> render(conditional, nullValue))
                .isInstanceOf(BizException.class)
                .hasMessage("必填参数值为空：isPark");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("isPark", 1 > 0);
        assertThat(objectMapper.writeValueAsString(params)).isEqualTo("{\"isPark\":true}");
    }

    @Test
    void shouldAllowBooleanParamConnectedDirectlyToAndOrNot() {
        ObjectNode booleanParam = paramBlock(12L, "isPark", ParamType.BOOLEAN);
        ObjectNode and = binary(BlocklyBlockTypes.LOGIC_OPERATION, "AND",
                "A", booleanParam,
                "B", paramBlock(12L, "isPark", ParamType.BOOLEAN));
        ObjectNode or = binary(BlocklyBlockTypes.LOGIC_OPERATION, "OR",
                "A", paramBlock(12L, "isPark", ParamType.BOOLEAN),
                "B", paramBlock(12L, "isPark", ParamType.BOOLEAN));
        ObjectNode not = unary(BlocklyBlockTypes.LOGIC_NEGATE, "BOOL",
                paramBlock(12L, "isPark", ParamType.BOOLEAN));

        ObjectNode andBranch = controlsIf(and, textBlock("AND_TRUE"));
        addElse(andBranch, textBlock("AND_FALSE"));
        ObjectNode orBranch = controlsIf(or, textBlock("OR_TRUE"));
        addElse(orBranch, textBlock("OR_FALSE"));
        ObjectNode notBranch = controlsIf(not, textBlock("NOT_TRUE"));
        addElse(notBranch, textBlock("NOT_FALSE"));

        assertThat(render(andBranch,
                Map.of("isPark", objectMapper.valueToTree(true))).renderedContent())
                .isEqualTo("AND_TRUE");
        assertThat(render(orBranch,
                Map.of("isPark", objectMapper.valueToTree(false))).renderedContent())
                .isEqualTo("OR_FALSE");
        assertThat(render(notBranch,
                Map.of("isPark", objectMapper.valueToTree(false))).renderedContent())
                .isEqualTo("NOT_TRUE");
    }

    @Test
    void shouldRejectNonBooleanAndOrNotInputsDuringTemplateValidation() {
        ObjectNode invalidAnd = binary(BlocklyBlockTypes.LOGIC_OPERATION, "AND",
                "A", textBlock("true"), "B", textBlock("false"));
        ObjectNode invalidNot = unary(BlocklyBlockTypes.LOGIC_NEGATE, "BOOL", textBlock("true"));

        assertThatThrownBy(() -> validate(controlsIf(invalidAnd, textBlock("DO0"))))
                .isInstanceOf(BizException.class)
                .hasMessage("logic_operation 的输入必须为布尔值");
        assertThatThrownBy(() -> validate(controlsIf(invalidNot, textBlock("DO0"))))
                .isInstanceOf(BizException.class)
                .hasMessage("logic_negate 的输入必须为布尔值");
    }

    @Test
    void shouldRenderNestedControlsIf() {
        ObjectNode outerCondition = compare("GT",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode innerCondition = compare("EQ",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(1L, "left", ParamType.NUMBER));
        ObjectNode inner = controlsIf(innerCondition, textBlock("INNER"));
        addElse(inner, textBlock("INNER_ELSE"));
        ObjectNode outer = controlsIf(outerCondition, inner);
        addElse(outer, textBlock("OUTER_ELSE"));

        BlocklyRenderResult result = render(outer, Map.of(
                "left", objectMapper.valueToTree(10),
                "right", objectMapper.valueToTree(4)
        ));

        assertThat(result.renderedContent()).isEqualTo("INNER");
        assertThat(result.usedParams()).containsExactly("left", "right");
    }

    @Test
    void shouldRejectInvalidControlsIfStructureAndTypes() {
        ObjectNode invalidCondition = controlsIf(textBlock("not boolean"), textBlock("DO0"));
        ObjectNode invalidBranch = controlsIf(
                compare("EQ",
                        paramBlock(1L, "left", ParamType.NUMBER),
                        paramBlock(2L, "right", ParamType.NUMBER)),
                paramBlock(1L, "left", ParamType.NUMBER));
        ObjectNode missingIf = objectMapper.createObjectNode();
        missingIf.put("type", BlocklyBlockTypes.CONTROLS_IF);
        putInput(missingIf, "DO0", textBlock("DO0"));
        ObjectNode missingDo = objectMapper.createObjectNode();
        missingDo.put("type", BlocklyBlockTypes.CONTROLS_IF);
        putInput(missingDo, "IF0", compare("EQ",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER)));

        assertThatThrownBy(() -> validate(invalidCondition))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 的 IF0 必须返回布尔值");
        assertThatThrownBy(() -> validate(invalidBranch))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 的 DO0 必须返回字符串");
        assertThatThrownBy(() -> validate(missingIf))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 缺少条件输入 IF0");
        assertThatThrownBy(() -> validate(missingDo))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 缺少分支输入 DO0");
    }

    @Test
    void shouldRejectControlsIfStateAndInputMismatch() {
        ObjectNode unexpectedElse = controlsIf(
                compare("EQ",
                        paramBlock(1L, "left", ParamType.NUMBER),
                        paramBlock(2L, "right", ParamType.NUMBER)),
                textBlock("DO0"));
        putInput(unexpectedElse, "ELSE", textBlock("ELSE"));

        ObjectNode unexpectedElseIf = controlsIf(
                compare("EQ",
                        paramBlock(1L, "left", ParamType.NUMBER),
                        paramBlock(2L, "right", ParamType.NUMBER)),
                textBlock("DO0"));
        putInput(unexpectedElseIf, "IF1", compare("EQ",
                paramBlock(1L, "left", ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER)));
        putInput(unexpectedElseIf, "DO1", textBlock("DO1"));

        ObjectNode tooManyBranches = controlsIf(
                compare("EQ",
                        paramBlock(1L, "left", ParamType.NUMBER),
                        paramBlock(2L, "right", ParamType.NUMBER)),
                textBlock("DO0"));
        tooManyBranches.putObject("extraState")
                .put("elseIfCount", 11)
                .put("hasElse", false);

        assertThatThrownBy(() -> validate(unexpectedElse))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 声明了 ELSE 分支但 hasElse 为 false");
        assertThatThrownBy(() -> validate(unexpectedElseIf))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 存在超出 elseIfCount 范围的输入 IF1");
        assertThatThrownBy(() -> validate(tooManyBranches))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_if 的 elseIfCount 不能超过 10");
    }

    @Test
    void shouldRenderStringArrayWithSeparatorAndMultipleLoopItems() {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", BlocklyBlockTypes.TEXT_JOIN);
        putInput(body, "ADD0", paramBlock(10L, "prefix", ParamType.STRING));
        putInput(body, "ADD1", loopItem(ParamType.STRING));
        putInput(body, "ADD2", textBlock("="));
        putInput(body, "ADD3", loopItem(ParamType.STRING));
        ObjectNode forEach = controlsForEach(
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                body,
                "，");

        BlocklyRenderResult result = render(forEach, Map.of(
                "stringItems", objectMapper.valueToTree(List.of("甲", "乙")),
                "prefix", objectMapper.valueToTree("P")
        ));

        assertThat(result.renderedContent()).isEqualTo("P甲=甲，P乙=乙");
        assertThat(result.usedParams()).containsExactly("stringItems", "prefix");
    }

    @Test
    void shouldRenderNumberArrayThroughFormatAndControlsIf() {
        ObjectNode loopNumber = loopItem(ParamType.NUMBER);
        ObjectNode condition = compare("GT", loopItem(ParamType.NUMBER),
                paramBlock(2L, "right", ParamType.NUMBER));
        ObjectNode formatted = unary(BlocklyBlockTypes.AMOUNT_FORMAT, "VALUE",
                loopItem(ParamType.NUMBER));
        ObjectNode branch = controlsIf(condition, formatted);
        addElse(branch, textBlock("小"));
        ObjectNode forEach = controlsForEach(
                paramBlock(9L, "numberItems", ParamType.NUMBER_ARRAY),
                branch,
                "|");

        BlocklyRenderResult result = render(forEach, Map.of(
                "numberItems", objectMapper.valueToTree(List.of(2, 6)),
                "right", objectMapper.valueToTree(4)
        ));

        assertThat(loopNumber.path("extraState").path("itemType").asText()).isEqualTo("NUMBER");
        assertThat(result.renderedContent()).isEqualTo("小|6.00");
        assertThat(result.usedParams()).containsExactly("numberItems", "right");
    }

    @Test
    void shouldRenderObjectArrayLoopItemFieldsAndEmptyMissingValues() {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", BlocklyBlockTypes.TEXT_JOIN);
        putInput(body, "ADD0", loopItemField("name", ParamType.STRING));
        putInput(body, "ADD1", textBlock("支付"));
        putInput(body, "ADD2", unary(BlocklyBlockTypes.AMOUNT_FORMAT, "VALUE",
                loopItemField("paid", ParamType.NUMBER)));
        putInput(body, "ADD3", textBlock("元"));
        putInput(body, "ADD4", loopItemField("missing", ParamType.STRING));
        putInput(body, "ADD5", loopItemField("remark", ParamType.STRING));
        ObjectNode forEach = controlsForEach(
                paramBlock(11L, "wallets", ParamType.OBJECT_ARRAY),
                body,
                "，");
        ObjectNode wallet = objectMapper.createObjectNode();
        wallet.put("name", "通用账户");
        wallet.put("paid", 10);
        wallet.putNull("remark");

        BlocklyRenderResult result = render(forEach, Map.of(
                "wallets", objectMapper.valueToTree(List.of(wallet))
        ));

        assertThat(result.renderedContent()).isEqualTo("通用账户支付10.00元");
        assertThat(result.usedParams()).containsExactly("wallets");
    }

    @Test
    void shouldRejectLoopItemFieldOutsideObjectArrayLoop() {
        ObjectNode invalidField = controlsForEach(
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                loopItemField("name", ParamType.STRING),
                "");

        assertThatThrownBy(() -> validate(loopItemField("name", ParamType.STRING)))
                .isInstanceOf(BizException.class)
                .hasMessage("loop_item_field 只能在对象数组循环体中使用");
        assertThatThrownBy(() -> validate(invalidField))
                .isInstanceOf(BizException.class)
                .hasMessage("loop_item_field 只能在对象数组循环体中使用");
    }

    @Test
    void shouldReturnEmptyForEmptyArrayAndRejectTooManyItems() {
        ObjectNode forEach = controlsForEach(
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                loopItem(ParamType.STRING),
                ",");
        List<String> tooMany = IntStream.rangeClosed(1, 101)
                .mapToObj(String::valueOf)
                .toList();

        assertThat(render(forEach, Map.of(
                "stringItems", objectMapper.valueToTree(List.of())
        )).renderedContent()).isEmpty();
        assertThatThrownBy(() -> render(forEach, Map.of(
                "stringItems", objectMapper.valueToTree(tooMany)
        )))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 的数组元素数量不能超过 100");
    }

    @Test
    void shouldRejectInvalidForEachStructureAndContext() {
        ObjectNode nonArray = controlsForEach(textBlock("not array"),
                textBlock("body"), "");
        ObjectNode nonStringBody = controlsForEach(
                paramBlock(9L, "numberItems", ParamType.NUMBER_ARRAY),
                loopItem(ParamType.NUMBER),
                "");
        ObjectNode mismatch = controlsForEach(
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                loopItem(ParamType.NUMBER),
                "");
        ObjectNode nested = controlsForEach(
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                controlsForEach(
                        paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                        loopItem(ParamType.STRING),
                        ""),
                "");
        ObjectNode missingList = objectMapper.createObjectNode();
        missingList.put("type", BlocklyBlockTypes.CONTROLS_FOR_EACH);
        putInput(missingList, "BODY", textBlock("body"));
        ObjectNode missingBody = objectMapper.createObjectNode();
        missingBody.put("type", BlocklyBlockTypes.CONTROLS_FOR_EACH);
        putInput(missingBody, "LIST",
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY));
        ObjectNode longSeparator = controlsForEach(
                paramBlock(8L, "stringItems", ParamType.STRING_ARRAY),
                loopItem(ParamType.STRING),
                "123456789012345678901");

        assertThatThrownBy(() -> validate(loopItem(ParamType.STRING)))
                .isInstanceOf(BizException.class)
                .hasMessage("loop_item_value 只能在循环体中使用");
        assertThatThrownBy(() -> validate(nonArray))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 的 LIST 必须返回数组");
        assertThatThrownBy(() -> validate(nonStringBody))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 的 BODY 必须返回字符串");
        assertThatThrownBy(() -> validate(mismatch))
                .isInstanceOf(BizException.class)
                .hasMessage("loop_item_value 的 itemType 与循环元素类型不一致");
        assertThatThrownBy(() -> validate(nested))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 暂不支持嵌套循环");
        assertThatThrownBy(() -> validate(missingList))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 缺少输入 LIST");
        assertThatThrownBy(() -> validate(missingBody))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 缺少输入 BODY");
        assertThatThrownBy(() -> validate(longSeparator))
                .isInstanceOf(BizException.class)
                .hasMessage("controls_forEach 的 SEPARATOR 长度不能超过 20");
    }

    private BlocklyRenderResult render(ObjectNode content, Map<String, JsonNode> values) {
        BlocklyValidationResult validation = validate(content);
        return actualRenderer.render(validation.getBlocklyJson(), 1L, expressionParams, values);
    }

    private BlocklyValidationResult validate(ObjectNode content) {
        return actualValidator.validateWorkspace(1, workspace(content), 1L,
                expressionParams, BlocklyValidationMode.DRAFT);
    }

    private Object evaluateExpression(ObjectNode expression,
                                      Map<String, JsonNode> values) throws Exception {
        validateBooleanExpression(expression);
        BlockRenderContext context = new BlockRenderContext(
                1L, expressionParams, values, new SceneParamValueValidator());
        Method renderNode = BlocklyRenderer.class.getDeclaredMethod(
                "renderNode", JsonNode.class, BlockRenderContext.class, boolean.class);
        renderNode.setAccessible(true);
        try {
            Object result = renderNode.invoke(actualRenderer, expression, context, false);
            Method value = result.getClass().getDeclaredMethod("value");
            value.setAccessible(true);
            return value.invoke(result);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        }
    }

    private void validateBooleanExpression(ObjectNode expression) {
        assertThatThrownBy(() -> validate(unary(BlocklyBlockTypes.LOGIC_NEGATE, "BOOL", expression)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("BOOLEAN 结果不能直接输出为模板正文");
    }

    private ObjectNode workspace(ObjectNode content) {
        ObjectNode workspace = objectMapper.createObjectNode();
        ObjectNode blocks = workspace.putObject("blocks");
        blocks.put("languageVersion", 0);
        ArrayNode topBlocks = blocks.putArray("blocks");
        ObjectNode root = topBlocks.addObject();
        root.put("type", BlocklyBlockTypes.MESSAGE_CONTENT);
        putInput(root, "CONTENT", content);
        return workspace;
    }

    private ObjectNode linkedWorkspace(ObjectNode... blocks) {
        ObjectNode workspace = objectMapper.createObjectNode();
        workspace.put("templateNodeMode", "LINKED_NODES");
        workspace.putArray("templateLinks");
        ArrayNode order = workspace.putArray("templateNodeOrder");
        ObjectNode blocksNode = workspace.putObject("blocks");
        blocksNode.put("languageVersion", 0);
        ArrayNode topBlocks = blocksNode.putArray("blocks");
        for (ObjectNode block : blocks) {
            order.add(block.path("id").asText());
            topBlocks.add(block);
        }
        if (blocks.length > 0) {
            workspace.put("templateEntryBlockId", blocks[0].path("id").asText());
        }
        return workspace;
    }

    private ObjectNode graphWorkspace(String entryBlockId, ObjectNode... blocks) {
        ObjectNode workspace = linkedWorkspace(blocks);
        workspace.put("templateEntryBlockId", entryBlockId);
        return workspace;
    }

    private void putLink(ObjectNode workspace, String sourceId, String targetId, String targetPort) {
        putLink(workspace, sourceId, "output", targetId, targetPort);
    }

    private void putLink(ObjectNode workspace,
                         String sourceId,
                         String sourcePort,
                         String targetId,
                         String targetPort) {
        ObjectNode link = ((ArrayNode) workspace.get("templateLinks")).addObject();
        link.put("sourceId", sourceId);
        link.put("sourcePort", sourcePort);
        link.put("targetId", targetId);
        link.put("targetPort", targetPort);
    }

    private void putLinkWithoutTargetPort(ObjectNode workspace, String sourceId, String targetId) {
        ObjectNode link = ((ArrayNode) workspace.get("templateLinks")).addObject();
        link.put("sourceId", sourceId);
        link.put("sourcePort", "output");
        link.put("targetId", targetId);
    }

    private void putMathExpression(ObjectNode workspace, String blockId, String leftBlockId, String rightBlockId) {
        ObjectNode expressions = workspace.has("templateMathExpressions")
                ? (ObjectNode) workspace.get("templateMathExpressions")
                : workspace.putObject("templateMathExpressions");
        expressions.putObject(blockId)
                .put("leftValueBlockId", leftBlockId)
                .put("rightValueBlockId", rightBlockId);
    }

    private ObjectNode binary(String type,
                              String operator,
                              String leftName,
                              ObjectNode left,
                              String rightName,
                              ObjectNode right) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", type);
        if (operator != null) {
            block.putObject("fields").put("OP", operator);
        }
        putInput(block, leftName, left);
        putInput(block, rightName, right);
        return block;
    }

    private ObjectNode unary(String type, String inputName, ObjectNode input) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", type);
        putInput(block, inputName, input);
        return block;
    }

    private ObjectNode compare(String operator, ObjectNode left, ObjectNode right) {
        return binary(BlocklyBlockTypes.LOGIC_COMPARE, operator, "A", left, "B", right);
    }

    private ObjectNode controlsIf(ObjectNode condition, ObjectNode branch) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.CONTROLS_IF);
        putInput(block, "IF0", condition);
        putInput(block, "DO0", branch);
        return block;
    }

    private ObjectNode controlsForEach(ObjectNode list,
                                       ObjectNode body,
                                       String separator) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.CONTROLS_FOR_EACH);
        if (separator != null) {
            block.putObject("fields").put("SEPARATOR", separator);
        }
        putInput(block, "LIST", list);
        putInput(block, "BODY", body);
        return block;
    }

    private ObjectNode loopItem(ParamType type) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.LOOP_ITEM_VALUE);
        block.putObject("extraState").put("itemType", type.getCode());
        return block;
    }

    private ObjectNode loopItemField(String fieldName, ParamType type) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.LOOP_ITEM_FIELD);
        ObjectNode extraState = block.putObject("extraState");
        extraState.put("fieldName", fieldName);
        extraState.put("fieldType", type.getCode());
        return block;
    }

    private void addElseIf(ObjectNode block,
                           int index,
                           ObjectNode condition,
                           ObjectNode branch) {
        ObjectNode extraState = block.has("extraState")
                ? (ObjectNode) block.get("extraState")
                : block.putObject("extraState");
        extraState.put("elseIfCount", index);
        extraState.put("hasElse", extraState.path("hasElse").asBoolean(false));
        putInput(block, "IF" + index, condition);
        putInput(block, "DO" + index, branch);
    }

    private void addElse(ObjectNode block, ObjectNode branch) {
        ObjectNode extraState = block.has("extraState")
                ? (ObjectNode) block.get("extraState")
                : block.putObject("extraState");
        extraState.put("elseIfCount", extraState.path("elseIfCount").asInt(0));
        extraState.put("hasElse", true);
        putInput(block, "ELSE", branch);
    }

    private void putInput(ObjectNode block, String inputName, ObjectNode input) {
        ObjectNode inputs = block.has("inputs")
                ? (ObjectNode) block.get("inputs")
                : block.putObject("inputs");
        inputs.putObject(inputName).set("block", input);
    }

    private ObjectNode textBlock(String value) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.TEXT);
        block.putObject("fields").put("TEXT", value);
        return block;
    }

    private ObjectNode textJoinBlock(String value) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.TEXT_JOIN);
        block.putObject("fields").put("TEXT", value);
        return block;
    }

    private ObjectNode linkedText(String id, String value) {
        ObjectNode block = textBlock(value);
        block.put("id", id);
        return block;
    }

    private ObjectNode linkedOperation(String id, String type, String operation) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", id);
        block.put("type", type);
        block.putObject("extraState").put("operation", operation);
        return block;
    }

    private ObjectNode linkedDecimals(String id, String type, int decimals) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", id);
        block.put("type", type);
        block.putObject("extraState").put("decimals", decimals);
        return block;
    }

    private ObjectNode linkedTimeFormat(String id, String format) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", id);
        block.put("type", BlocklyBlockTypes.TIME_FORMAT);
        block.putObject("fields").put("FORMAT", format);
        return block;
    }

    private ObjectNode mathNumber(String id, Object value) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", id);
        block.put("type", BlocklyBlockTypes.MATH_NUMBER);
        block.putObject("fields").set("NUM", objectMapper.valueToTree(value));
        return block;
    }

    private ObjectNode paramBlock(Long id, String name, ParamType type) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", BlocklyBlockTypes.SCENE_PARAM_VALUE);
        ObjectNode extraState = block.putObject("extraState");
        extraState.put("sceneId", "1");
        extraState.put("paramId", id.toString());
        extraState.put("paramName", name);
        extraState.put("paramType", type.getCode());
        return block;
    }

    private ObjectNode sceneParamBlock(String blockType,
                                       String blockId,
                                       String sceneId,
                                       String paramId,
                                       String paramName,
                                       String paramLabel,
                                       ParamType paramType) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", blockId);
        block.put("type", blockType);
        ObjectNode extraState = block.putObject("extraState");
        extraState.put("sceneId", sceneId);
        extraState.put("paramId", paramId);
        extraState.put("paramName", paramName);
        extraState.put("paramLabel", paramLabel);
        extraState.put("paramType", paramType.getCode());
        return block;
    }

    private void assertReboundParam(JsonNode extraState,
                                    String sceneId,
                                    String paramId,
                                    String paramName,
                                    String paramLabel,
                                    ParamType paramType) {
        assertThat(extraState.path("sceneId").isTextual()).isTrue();
        assertThat(extraState.path("paramId").isTextual()).isTrue();
        assertThat(extraState.path("sceneId").asText()).isEqualTo(sceneId);
        assertThat(extraState.path("paramId").asText()).isEqualTo(paramId);
        assertThat(extraState.path("paramName").asText()).isEqualTo(paramName);
        assertThat(extraState.path("paramLabel").asText()).isEqualTo(paramLabel);
        assertThat(extraState.path("paramType").asText()).isEqualTo(paramType.getCode());
    }

    private TemplateCreateDTO templateCreateRequest(Long sceneId) {
        TemplateCreateDTO request = new TemplateCreateDTO();
        request.setTemplateName("测试模板");
        request.setSceneId(sceneId);
        request.setChannelType(ChannelType.SMS.getCode());
        request.setUnitIds(List.of());
        return request;
    }

    private MsgTemplateServiceImpl actualTemplateService() {
        return new MsgTemplateServiceImpl(
                msgTemplateMapper,
                msgTemplateUnitMapper,
                msgSceneMapper,
                msgSceneParamMapper,
                actualValidator,
                blocklyRenderer);
    }

    private void initializeTemplateTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MsgTemplate.class);
        TableInfoHelper.initTableInfo(assistant, MsgSceneParam.class);
    }

    private MsgScene enabledScene(Long id) {
        MsgScene scene = new MsgScene();
        scene.setId(id);
        scene.setStatus(CommonStatus.ENABLE.getCode());
        return scene;
    }

    private MsgTemplate sourceTemplate(Long sceneId, String blocklyJson) {
        MsgTemplate template = new MsgTemplate();
        template.setId(10L);
        template.setSceneId(sceneId);
        template.setTemplateName("源模板");
        template.setChannelType(ChannelType.SMS.getCode());
        template.setBlocklyJson(blocklyJson);
        template.setStatus(CommonStatus.DISABLE.getCode());
        return template;
    }

    private TemplateCopyDTO copyRequest(Long sceneId, boolean copyContent) {
        TemplateCopyDTO request = new TemplateCopyDTO();
        request.setTemplateName("复制模板");
        request.setSceneId(sceneId);
        request.setChannelType(ChannelType.SMS.getCode());
        request.setCopyContent(copyContent);
        request.setUnitIds(List.of());
        return request;
    }

    private String storedContent(Long sceneId,
                                 Map<Long, MsgSceneParam> params,
                                 ObjectNode... blocks) {
        BlocklyValidationResult validation = actualValidator.validateWorkspace(
                1, linkedWorkspace(blocks), sceneId, params, BlocklyValidationMode.DRAFT);
        return actualValidator.write(validation.getBlocklyJson());
    }

    private MsgSceneParam param(Long id, String name, ParamType type) {
        return sceneParam(id, 1L, name, null, type);
    }

    private MsgSceneParam sceneParam(Long id,
                                     Long sceneId,
                                     String name,
                                     String label,
                                     ParamType type) {
        MsgSceneParam param = new MsgSceneParam();
        param.setId(id);
        param.setSceneId(sceneId);
        param.setParamName(name);
        param.setParamLabel(label);
        param.setParamType(type.getCode());
        param.setIsRequired(1);
        return param;
    }

    private MsgSceneParam optionalParam(Long id, String name, ParamType type) {
        MsgSceneParam param = param(id, name, type);
        param.setIsRequired(0);
        return param;
    }
}
