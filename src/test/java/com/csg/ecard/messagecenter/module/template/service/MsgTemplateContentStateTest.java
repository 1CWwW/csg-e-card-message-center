package com.csg.ecard.messagecenter.module.template.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.TemplateContentStatus;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyBlockTypes;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationMode;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyValidationResult;
import com.csg.ecard.messagecenter.module.template.dto.TemplateContentSaveDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCopyDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.mapper.TemplateQueryRow;
import com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl;
import com.csg.ecard.messagecenter.module.template.vo.TemplateCopyVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateOverviewVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 模板复制渠道与 Blockly 内容状态回归测试。
 */
@ExtendWith(MockitoExtension.class)
class MsgTemplateContentStateTest {

    private static final Long SOURCE_ID = 10L;
    private static final Long COPIED_ID = 20L;
    private static final Long SCENE_ID = 1L;

    @Mock
    private MsgTemplateMapper msgTemplateMapper;
    @Mock
    private MsgTemplateUnitMapper msgTemplateUnitMapper;
    @Mock
    private MsgSceneMapper msgSceneMapper;
    @Mock
    private MsgSceneParamMapper msgSceneParamMapper;
    @Mock
    private BlocklyRenderer blocklyRenderer;

    private ObjectMapper objectMapper;
    private BlocklyJsonValidator blocklyJsonValidator;
    private MsgTemplateServiceImpl msgTemplateService;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MsgTemplate.class);
        TableInfoHelper.initTableInfo(assistant, MsgSceneParam.class);
        objectMapper = new ObjectMapper();
        blocklyJsonValidator = new BlocklyJsonValidator(objectMapper);
        msgTemplateService = new MsgTemplateServiceImpl(
                msgTemplateMapper,
                msgTemplateUnitMapper,
                msgSceneMapper,
                msgSceneParamMapper,
                blocklyJsonValidator,
                blocklyRenderer);
    }

    @Test
    void shouldCopySmsTemplateToRequestedEmailChannelAndReturnPersistedResult() {
        Map<Long, MsgTemplate> store = new HashMap<>();
        store.put(SOURCE_ID, template(SOURCE_ID, ChannelType.SMS.getCode(), validContent()));
        MsgScene scene = enabledScene();
        when(msgTemplateMapper.selectById(anyLong())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        when(msgSceneMapper.selectById(SCENE_ID)).thenReturn(scene);
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate copied = invocation.getArgument(0);
            copied.setId(COPIED_ID);
            store.put(COPIED_ID, copied);
            return 1;
        });
        when(msgTemplateMapper.selectTemplateDetail(COPIED_ID))
                .thenAnswer(invocation -> queryRow(store.get(COPIED_ID), scene));
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(COPIED_ID)).thenReturn(List.of("unit-1"));

        TemplateCopyVO result = msgTemplateService.copy(
                SOURCE_ID, copyRequest(ChannelType.EMAIL.getCode(), true, List.of("unit-1")));
        TemplateDetailVO detail = msgTemplateService.detail(COPIED_ID);

        assertThat(result.getNewTemplateId()).isEqualTo(COPIED_ID);
        assertThat(result.getSceneId()).isEqualTo(SCENE_ID);
        assertThat(result.getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
        assertThat(result.getHasContent()).isTrue();
        assertThat(detail.getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
        assertThat(detail.getHasContent()).isTrue();
        assertThat(objectMapper.valueToTree(result).path("newTemplateId").isTextual()).isTrue();
        assertThat(objectMapper.valueToTree(result).path("sceneId").isTextual()).isTrue();

        ArgumentCaptor<MsgTemplate> copiedCaptor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).insert(copiedCaptor.capture());
        assertThat(copiedCaptor.getValue().getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
        assertThat(copiedCaptor.getValue().getBlocklyJson())
                .isEqualTo(store.get(SOURCE_ID).getBlocklyJson())
                .isNotSameAs(store.get(SOURCE_ID).getBlocklyJson());
    }

    @Test
    void shouldCreateEmptyCopyWhenCopyContentIsFalse() {
        when(msgTemplateMapper.selectById(SOURCE_ID))
                .thenReturn(template(SOURCE_ID, ChannelType.SMS.getCode(), validContent()));
        when(msgSceneMapper.selectById(SCENE_ID)).thenReturn(enabledScene());
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            invocation.<MsgTemplate>getArgument(0).setId(COPIED_ID);
            return 1;
        });

        TemplateCopyVO result = msgTemplateService.copy(
                SOURCE_ID, copyRequest(ChannelType.EMAIL.getCode(), false, List.of()));

        assertThat(result.getHasContent()).isFalse();
        ArgumentCaptor<MsgTemplate> copiedCaptor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).insert(copiedCaptor.capture());
        assertThat(copiedCaptor.getValue().getBlocklyJson()).isNull();
        verify(msgSceneParamMapper, never()).selectList(any());
    }

    @Test
    void shouldCreateEmptyCopyWhenSourceOnlyContainsEditorMetadata() {
        when(msgTemplateMapper.selectById(SOURCE_ID))
                .thenReturn(template(SOURCE_ID, ChannelType.SMS.getCode(), emptyContent()));
        when(msgSceneMapper.selectById(SCENE_ID)).thenReturn(enabledScene());
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());
        when(msgTemplateMapper.insert(any(MsgTemplate.class))).thenAnswer(invocation -> {
            invocation.<MsgTemplate>getArgument(0).setId(COPIED_ID);
            return 1;
        });

        TemplateCopyVO result = msgTemplateService.copy(
                SOURCE_ID, copyRequest(ChannelType.EMAIL.getCode(), true, List.of()));

        assertThat(result.getHasContent()).isFalse();
        ArgumentCaptor<MsgTemplate> copiedCaptor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).insert(copiedCaptor.capture());
        assertThat(copiedCaptor.getValue().getBlocklyJson()).isNull();
    }

    @Test
    void shouldSaveEmptyCanvasAsValidAndExposeUneditedStateEverywhere() {
        AtomicReference<MsgTemplate> state = new AtomicReference<>(
                template(SOURCE_ID, ChannelType.SMS.getCode(), validContent()));
        MsgScene scene = enabledScene();
        stubMutableTemplate(state);
        when(msgSceneMapper.selectById(SCENE_ID)).thenReturn(scene);
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());
        when(msgTemplateMapper.selectTemplateDetail(SOURCE_ID))
                .thenAnswer(invocation -> queryRow(state.get(), scene));
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(SOURCE_ID)).thenReturn(List.of());
        when(msgTemplateMapper.selectTemplatePage(any(), any())).thenAnswer(invocation -> {
            Page<TemplateQueryRow> page = invocation.getArgument(0);
            page.setRecords(List.of(queryRow(state.get(), scene)));
            page.setTotal(1L);
            return page;
        });
        when(msgTemplateMapper.selectTemplateList(any()))
                .thenAnswer(invocation -> List.of(queryRow(state.get(), scene)));
        when(msgTemplateMapper.selectOverviewTemplates())
                .thenAnswer(invocation -> List.of(queryRow(state.get(), scene)));

        var saved = msgTemplateService.saveContent(SOURCE_ID, contentRequest(emptyWorkspace()));
        PageResult<TemplateListVO> list = msgTemplateService.page(pageQuery(0));
        TemplateDetailVO detail = msgTemplateService.detail(SOURCE_ID);
        TemplateOverviewVO overview = msgTemplateService.overview();
        PageResult<TemplateListVO> unedited = msgTemplateService.page(pageQuery(2));
        PageResult<TemplateListVO> edited = msgTemplateService.page(pageQuery(1));

        assertThat(saved.getHasContent()).isFalse();
        assertThat(saved.getValid()).isTrue();
        assertThat(saved.getErrors()).isEmpty();
        assertThat(state.get().getBlocklyJson()).isNotBlank();
        assertThat(list.getList()).singleElement().satisfies(item -> assertUnedited(item));
        assertThat(detail.getHasContent()).isFalse();
        assertThat(detail.getBlocklyJson().path("workspace").path("templateNodeOrder").isEmpty()).isTrue();
        assertThat(overview.getTotal()).isEqualTo(1L);
        assertThat(overview.getEditedCount()).isZero();
        assertThat(overview.getPendingCount()).isEqualTo(1L);
        assertThat(unedited.getList()).singleElement().satisfies(item -> assertUnedited(item));
        assertThat(unedited.getTotal()).isEqualTo(1L);
        assertThat(edited.getList()).isEmpty();
        assertThat(edited.getTotal()).isZero();
    }

    @Test
    void shouldSaveValidBlocksAndExposeEditedState() {
        AtomicReference<MsgTemplate> state = new AtomicReference<>(
                template(SOURCE_ID, ChannelType.SMS.getCode(), null));
        MsgScene scene = enabledScene();
        stubMutableTemplate(state);
        when(msgSceneMapper.selectById(SCENE_ID)).thenReturn(scene);
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());
        when(msgTemplateMapper.selectTemplatePage(any(), any())).thenAnswer(invocation -> {
            Page<TemplateQueryRow> page = invocation.getArgument(0);
            page.setRecords(List.of(queryRow(state.get(), scene)));
            page.setTotal(1L);
            return page;
        });

        var saved = msgTemplateService.saveContent(SOURCE_ID, contentRequest(validWorkspace()));
        PageResult<TemplateListVO> list = msgTemplateService.page(pageQuery(0));

        assertThat(saved.getHasContent()).isTrue();
        assertThat(saved.getValid()).isTrue();
        assertThat(saved.getErrors()).isEmpty();
        assertThat(list.getList()).singleElement().satisfies(item -> {
            assertThat(item.getHasContent()).isTrue();
            assertThat(item.getContentStatusDesc()).isEqualTo(TemplateContentStatus.EDITED.getDesc());
        });
    }

    @Test
    void shouldPersistChannelUpdateAndReturnItFromSubsequentDetailQuery() {
        AtomicReference<MsgTemplate> state = new AtomicReference<>(
                template(SOURCE_ID, ChannelType.SMS.getCode(), null));
        MsgScene scene = enabledScene();
        stubMutableTemplate(state);
        when(msgSceneMapper.selectById(SCENE_ID)).thenReturn(scene);
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateMapper.selectTemplateDetail(SOURCE_ID))
                .thenAnswer(invocation -> queryRow(state.get(), scene));
        when(msgTemplateUnitMapper.selectUnitIdsByTemplateId(SOURCE_ID)).thenReturn(List.of());
        when(msgSceneParamMapper.selectList(any())).thenReturn(List.of());
        TemplateUpdateDTO request = new TemplateUpdateDTO();
        request.setTemplateName("渠道更新模板");
        request.setSceneId(SCENE_ID);
        request.setChannelType(ChannelType.EMAIL.getCode());
        request.setStatus(CommonStatus.DISABLE.getCode());
        request.setUnitIds(List.of());

        TemplateDetailVO updated = msgTemplateService.update(SOURCE_ID, request);
        TemplateDetailVO queried = msgTemplateService.detail(SOURCE_ID);

        assertThat(state.get().getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
        assertThat(updated.getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
        assertThat(queried.getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
    }

    @Test
    void templateManagementListShouldOrderByLatestUpdateTime() throws Exception {
        Select pageSelect = MsgTemplateMapper.class
                .getMethod("selectTemplatePage", Page.class, TemplatePageQueryDTO.class)
                .getAnnotation(Select.class);
        Select contentStatusSelect = MsgTemplateMapper.class
                .getMethod("selectTemplateList", TemplatePageQueryDTO.class)
                .getAnnotation(Select.class);

        assertLatestUpdateFirst(pageSelect);
        assertLatestUpdateFirst(contentStatusSelect);
    }

    @Test
    void shouldDocumentCopyChannelTypeAsRequiredEnum() throws Exception {
        Field channelTypeField = TemplateCopyDTO.class.getDeclaredField("channelType");
        NotBlank notBlank = channelTypeField.getAnnotation(NotBlank.class);
        Schema schema = channelTypeField.getAnnotation(Schema.class);

        assertThat(notBlank).isNotNull();
        assertThat(schema).isNotNull();
        assertThat(schema.requiredMode()).isEqualTo(Schema.RequiredMode.REQUIRED);
        assertThat(schema.allowableValues())
                .containsExactly("SMS", "EMAIL", "ELINK", "IN_APP");
    }

    private void stubMutableTemplate(AtomicReference<MsgTemplate> state) {
        when(msgTemplateMapper.selectById(SOURCE_ID)).thenAnswer(invocation -> state.get());
        when(msgTemplateMapper.updateById(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate update = invocation.getArgument(0);
            MsgTemplate current = state.get();
            if (update.getTemplateName() != null) {
                current.setTemplateName(update.getTemplateName());
            }
            if (update.getSceneId() != null) {
                current.setSceneId(update.getSceneId());
            }
            if (update.getChannelType() != null) {
                current.setChannelType(update.getChannelType());
            }
            if (update.getBlocklyJson() != null) {
                current.setBlocklyJson(update.getBlocklyJson());
            }
            if (update.getStatus() != null) {
                current.setStatus(update.getStatus());
            }
            return 1;
        });
    }

    private MsgTemplate template(Long id, String channelType, String blocklyJson) {
        MsgTemplate template = new MsgTemplate();
        template.setId(id);
        template.setTemplateName("源模板");
        template.setSceneId(SCENE_ID);
        template.setChannelType(channelType);
        template.setBlocklyJson(blocklyJson);
        template.setStatus(CommonStatus.DISABLE.getCode());
        return template;
    }

    private MsgScene enabledScene() {
        MsgScene scene = new MsgScene();
        scene.setId(SCENE_ID);
        scene.setSceneCode("SCENE_1");
        scene.setSceneName("测试场景");
        scene.setStatus(CommonStatus.ENABLE.getCode());
        return scene;
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

    private TemplateCopyDTO copyRequest(String channelType, boolean copyContent, List<String> unitIds) {
        TemplateCopyDTO request = new TemplateCopyDTO();
        request.setTemplateName("复制模板");
        request.setSceneId(SCENE_ID);
        request.setChannelType(channelType);
        request.setCopyContent(copyContent);
        request.setUnitIds(unitIds);
        return request;
    }

    private TemplateContentSaveDTO contentRequest(JsonNode workspace) {
        TemplateContentSaveDTO request = new TemplateContentSaveDTO();
        request.setSchemaVersion(1);
        request.setWorkspace(workspace);
        return request;
    }

    private TemplatePageQueryDTO pageQuery(int contentStatus) {
        TemplatePageQueryDTO query = new TemplatePageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setContentStatus(contentStatus);
        return query;
    }

    private String validContent() {
        BlocklyValidationResult validation = blocklyJsonValidator.validateWorkspace(
                1, validWorkspace(), SCENE_ID, Map.of(), BlocklyValidationMode.DRAFT);
        return blocklyJsonValidator.write(validation.getBlocklyJson());
    }

    private String emptyContent() {
        BlocklyValidationResult validation = blocklyJsonValidator.validateWorkspace(
                1, emptyWorkspace(), SCENE_ID, Map.of(), BlocklyValidationMode.DRAFT);
        return blocklyJsonValidator.write(validation.getBlocklyJson());
    }

    private ObjectNode emptyWorkspace() {
        ObjectNode workspace = objectMapper.createObjectNode();
        workspace.put("templateNodeMode", "LINKED_NODES");
        workspace.putArray("templateUiLinks");
        workspace.put("templateEntryBlockId", "");
        workspace.putArray("templateNodeOrder");
        return workspace;
    }

    private ObjectNode validWorkspace() {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("id", "text-1");
        block.put("type", BlocklyBlockTypes.TEXT);
        block.putObject("fields").put("TEXT", "有效内容");

        ObjectNode workspace = objectMapper.createObjectNode();
        workspace.put("templateNodeMode", "LINKED_NODES");
        workspace.putArray("templateLinks");
        workspace.putArray("templateNodeOrder").add("text-1");
        workspace.put("templateEntryBlockId", "text-1");
        ObjectNode blocks = workspace.putObject("blocks");
        blocks.put("languageVersion", 0);
        ArrayNode topBlocks = blocks.putArray("blocks");
        topBlocks.add(block);
        return workspace;
    }

    private void assertUnedited(TemplateListVO item) {
        assertThat(item.getHasContent()).isFalse();
        assertThat(item.getContentStatusDesc()).isEqualTo(TemplateContentStatus.EMPTY.getDesc());
    }

    private void assertLatestUpdateFirst(Select select) {
        String sql = String.join("\n", select.value());
        assertThat(sql).contains("ORDER BY t.update_time DESC, t.id DESC");
        assertThat(sql).doesNotContain("ORDER BY t.create_time ASC");
    }
}
