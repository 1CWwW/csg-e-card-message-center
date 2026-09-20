package com.csg.ecard.messagecenter.module.push.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.csg.ecard.messagecenter.common.utils.MessageIdGenerator;
import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import com.csg.ecard.messagecenter.config.message.MessageRecordProperties;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbDecision;
import com.csg.ecard.messagecenter.module.dnd.service.DoNotDisturbPolicyService;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.entity.MsgRecord;
import com.csg.ecard.messagecenter.module.push.enums.PushStatus;
import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import com.csg.ecard.messagecenter.module.push.mapper.MsgRecordMapper;
import com.csg.ecard.messagecenter.module.push.mq.AsyncPushMessage;
import com.csg.ecard.messagecenter.module.push.sender.*;
import com.csg.ecard.messagecenter.module.push.service.impl.MessagePushServiceImpl;
import com.csg.ecard.messagecenter.module.scene.entity.*;
import com.csg.ecard.messagecenter.module.scene.mapper.*;
import com.csg.ecard.messagecenter.module.template.blockly.*;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePreviewDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static com.csg.ecard.messagecenter.module.template.rule.RuleFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 通过现有同步发送入口验证渲染、分发和失败记录，不调用真实渠道。 */
class RuleTemplateSendTest {
    private final Map<Class<?>, Object> dependencies = new HashMap<>();
    private final BlocklyJsonValidator validator = new BlocklyJsonValidator(MAPPER);
    private MessagePushServiceImpl service;
    private MsgTemplate template;
    private <T> T dependency(Class<T> type) { return type.cast(dependencies.get(type)); }

    @BeforeEach void setup() throws Exception {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        for (Class<?> type : List.of(MsgScene.class, MsgSceneParam.class, MsgTemplate.class, MsgRecord.class)) TableInfoHelper.initTableInfo(assistant, type);
        var constructor = MessagePushServiceImpl.class.getConstructors()[0];
        for (Class<?> type : constructor.getParameterTypes()) dependencies.put(type, mock(type));
        dependencies.put(ObjectMapper.class, MAPPER);
        dependencies.put(BlocklyJsonValidator.class, validator);
        dependencies.put(BlocklyRenderer.class, new BlocklyRenderer(new SceneParamValueValidator()));
        dependencies.put(SceneParamValueValidator.class, new SceneParamValueValidator());
        dependencies.put(MessageRecordProperties.class, new MessageRecordProperties());
        service = (MessagePushServiceImpl)constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(dependencies::get).toArray());
        MsgScene scene = new MsgScene(); scene.setId(1L); scene.setSceneCode("TEST"); scene.setSceneName("测试场景"); scene.setStatus(1);
        when(dependency(MsgSceneMapper.class).selectOne(any())).thenReturn(scene);
        when(dependency(MsgSceneMapper.class).selectById(1L)).thenReturn(scene);
        when(dependency(MsgSceneParamMapper.class).selectList(any())).thenReturn(new ArrayList<>(params().values()));
        template = new MsgTemplate(); template.setId(10L); template.setSceneId(1L); template.setStatus(1); template.setChannelType("SMS"); template.setTemplateName("测试模板");
        setRule(draft());
        when(dependency(MsgTemplateMapper.class).selectById(10L)).thenAnswer(i -> template);
        when(dependency(MsgTemplateMapper.class).selectEnabledDefaultTemplates(1L,"SMS")).thenAnswer(i -> List.of(template));
        MsgChannel channel = new MsgChannel(); channel.setId(20L); channel.setChannelType("SMS"); channel.setStatus(1);
        when(dependency(MsgChannelMapper.class).selectEnabledDefaultCandidates("SMS")).thenReturn(List.of(channel));
        when(dependency(MessageIdGenerator.class).nextId()).thenReturn("test-message-id");
        when(dependency(DoNotDisturbPolicyService.class).evaluate(any(), any(), any(), any()))
                .thenReturn(new DoNotDisturbDecision(false, null, null));
        when(dependency(ChannelSenderDispatcher.class).hasSender("SMS")).thenReturn(true);
        when(dependency(ChannelSenderDispatcher.class).dispatch(eq("SMS"), any())).thenReturn(ChannelSendResult.succeeded());
    }
    private void setRule(ObjectNode rule) {
        ObjectNode workspace = MAPPER.createObjectNode(); workspace.set("ruleTemplate", rule.deepCopy());
        workspace.putArray("obsoleteNodes").add("不要发送");
        template.setBlocklyJson(validator.write(validator.ruleEnvelope(rule, workspace,"10",1L,params())));
    }
    private void setLinearTimeTemplate(String pattern) {
        ObjectNode workspace = MAPPER.createObjectNode();
        workspace.put("templateNodeMode", "LINKED_NODES");
        workspace.putArray("templateLinks");
        workspace.putArray("templateNodeOrder").add("time-param").add("time-format");
        ObjectNode blocks = workspace.putObject("blocks");
        blocks.put("languageVersion", 0);
        ObjectNode param = blocks.putArray("blocks").addObject();
        param.put("id", "time-param");
        param.put("type", BlocklyBlockTypes.SCENE_PARAM_VALUE);
        param.putObject("extraState")
                .put("sceneId", "1")
                .put("paramId", "4")
                .put("paramName", "p4")
                .put("paramType", "TIME");
        ObjectNode formatter = blocks.withArray("blocks").addObject();
        formatter.put("id", "time-format");
        formatter.put("type", BlocklyBlockTypes.TIME_FORMAT);
        formatter.putObject("fields").put("FORMAT", pattern);
        BlocklyValidationResult validation = validator.validateWorkspace(
                1, workspace, 1L, params(), BlocklyValidationMode.ENABLE);
        template.setBlocklyJson(validator.write(validation.getBlocklyJson()));
    }
    private SyncPushDTO request(Map<String,Object> values) {
        SyncPushDTO request = new SyncPushDTO(); request.setSceneCode("TEST"); request.setUserId("test-user");
        request.setUserPhone("13800000000"); request.setSceneParams(values); return request;
    }
    @Test void ruleSendUsesSameCustomDateFormattingAsPreview() {
        ObjectNode rule = draft();
        ((ObjectNode)rule.path("versions").get(0)).set("content", content("{{t}}",
                binding("t","param","4","TIME","date",0).put("datePattern", "yyyy年MM月dd日")));
        setRule(rule);
        TemplatePreviewDTO preview = new TemplatePreviewDTO();
        preview.setTemplateId("10");
        preview.setEditorType("RULE_VERSIONS");
        preview.setValues(Map.of("p1", MAPPER.valueToTree(0),
                "p4", MAPPER.valueToTree("2026-11-12 11:12:13.123")));
        String previewContent = new com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl(
                dependency(MsgTemplateMapper.class),
                dependency(com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper.class),
                dependency(MsgSceneMapper.class), dependency(MsgSceneParamMapper.class), validator,
                new BlocklyRenderer(new SceneParamValueValidator())).preview(preview).getContent();

        var response = service.pushSync(request(Map.of("p1",0,"p4","2026-11-12 11:12:13.123")));
        assertThat(response.getChannelResults()).singleElement().satisfies(r -> {
            assertThat(r.getMessageContent()).isEqualTo("2026年11月12日");
            assertThat(r.getMessageContent()).isEqualTo(previewContent);
            assertThat(r.getStatus()).isEqualTo(SendStatus.SUCCESS);
        });
        ArgumentCaptor<ChannelSendRequest> sent = ArgumentCaptor.forClass(ChannelSendRequest.class);
        verify(dependency(ChannelSenderDispatcher.class)).dispatch(eq("SMS"), sent.capture());
        assertThat(sent.getValue().messageContent()).isEqualTo("2026年11月12日");
    }
    @Test void ruleCalculationsAreSameForPreviewSendAndRetry() {
        ObjectNode rule = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "money", 2);
        amount.putArray("calculations")
                .add(calculationReference("add", "ADD", "2"))
                .add(calculation("minus", "MINUS", "50").put("currentSide", "right"));
        ((ObjectNode) rule.path("versions").get(0))
                .set("content", content("金额{{amount}}", amount));
        setRule(rule);

        TemplatePreviewDTO preview = new TemplatePreviewDTO();
        preview.setTemplateId("10");
        preview.setEditorType("RULE_VERSIONS");
        preview.setValues(Map.of("p1", MAPPER.valueToTree(10), "p2", MAPPER.valueToTree(2)));
        String previewContent = new com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl(
                dependency(MsgTemplateMapper.class),
                dependency(com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper.class),
                dependency(MsgSceneMapper.class), dependency(MsgSceneParamMapper.class), validator,
                new BlocklyRenderer(new SceneParamValueValidator())).preview(preview).getContent();
        assertThat(previewContent).isEqualTo("金额38.00");

        SyncPushDTO request = request(Map.of("p1", 10, "p2", 2));
        var response = service.pushSync(request);
        assertThat(response.getChannelResults()).singleElement()
                .satisfies(result -> assertThat(result.getMessageContent()).isEqualTo(previewContent));

        clearInvocations(dependency(ChannelSenderDispatcher.class));
        AsyncPushMessage retry = new AsyncPushMessage("retry-message", request, MessageCallType.ASYNC);
        retry.setRetryCount(1);
        retry.setPendingTemplateIds(List.of(10L));
        assertThat(service.consumeAsync(retry).requiresRetry()).isFalse();
        ArgumentCaptor<ChannelSendRequest> sent = ArgumentCaptor.forClass(ChannelSendRequest.class);
        verify(dependency(ChannelSenderDispatcher.class)).dispatch(eq("SMS"), sent.capture());
        assertThat(sent.getValue().messageContent()).isEqualTo(previewContent);
    }
    @Test void skipFallbackPreviewAndDeliveryFinishNormallyWithoutRecordOrRetry() {
        ObjectNode rule = draft();
        ((ObjectNode) rule.path("versions").get(0)).set("condition", group("all",
                condition("skip-condition", "param", "1", "NUMBER", "eq", "1")));
        ObjectNode fallback = (ObjectNode) rule.path("fallback");
        fallback.put("action", "SKIP");
        fallback.remove("content");
        setRule(rule);

        TemplatePreviewDTO preview = new TemplatePreviewDTO();
        preview.setTemplateId("10");
        preview.setEditorType("RULE_VERSIONS");
        preview.setValues(Map.of("p1", MAPPER.valueToTree(0)));
        var previewResult = new com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl(
                dependency(MsgTemplateMapper.class),
                dependency(com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper.class),
                dependency(MsgSceneMapper.class), dependency(MsgSceneParamMapper.class), validator,
                new BlocklyRenderer(new SceneParamValueValidator())).preview(preview);
        assertThat(previewResult.isSkipSend()).isTrue();
        assertThat(previewResult.getContent()).isEmpty();
        assertThat(previewResult.getRenderedContent()).isEmpty();
        assertThat(previewResult.getWarnings())
                .containsExactly(com.csg.ecard.messagecenter.module.template.rule.RuleTemplateEngine.SKIP_SEND_MESSAGE);

        SyncPushDTO request = request(Map.of("p1", 0));
        var response = service.pushSync(request);
        assertThat(response.getStatus()).isEqualTo(PushStatus.SUCCESS);
        assertThat(response.getChannelResults()).singleElement().satisfies(result -> {
            assertThat(result.getStatus()).isEqualTo(SendStatus.SUCCESS);
            assertThat(result.getMessageContent()).isEmpty();
            assertThat(result.getResultMsg())
                    .isEqualTo(com.csg.ecard.messagecenter.module.template.rule.RuleTemplateEngine.SKIP_SEND_MESSAGE);
        });

        AsyncPushMessage retry = new AsyncPushMessage("skip-retry", request, MessageCallType.ASYNC);
        retry.setRetryCount(1);
        retry.setPendingTemplateIds(List.of(10L));
        assertThat(service.consumeAsync(retry).requiresRetry()).isFalse();
        verify(dependency(ChannelSenderDispatcher.class), never()).dispatch(anyString(), any());
        verify(dependency(MsgRecordMapper.class), never()).insert(any(MsgRecord.class));
        verify(dependency(MsgRecordMapper.class), never()).updateById(any(MsgRecord.class));
    }
    @Test void conversionFailureRecordsFailureWithoutDispatchingPartialMessage() {
        ObjectNode rule = draft(); ((ObjectNode)rule.path("versions").get(0)).set("content", content("prefix{{n}}", binding("n","param","2","NUMBER","money",2))); setRule(rule);
        var response = service.pushSync(request(Map.of("p1",0,"p2","invalid")));
        assertThat(response.getChannelResults()).singleElement().satisfies(r -> {
            assertThat(r.getStatus()).isEqualTo(SendStatus.FAILED); assertThat(r.getMessageContent()).isNullOrEmpty(); assertThat(r.getErrorMsg()).contains("{{n}}");
        });
        verify(dependency(ChannelSenderDispatcher.class), never()).dispatch(anyString(), any());
        ArgumentCaptor<MsgRecord> record = ArgumentCaptor.forClass(MsgRecord.class);
        verify(dependency(MsgRecordMapper.class)).insert(record.capture());
        assertThat(record.getValue().getSendStatus()).isEqualTo("FAILED"); assertThat(record.getValue().getMessageContent()).isNullOrEmpty();
    }
    @Test void historicalBlocklyStillRendersAndUsesExistingDispatcher() throws Exception {
        ObjectNode workspace = (ObjectNode)MAPPER.readTree("""
            {"blocks":{"languageVersion":0,"blocks":[{"type":"message_content","id":"root","inputs":{"CONTENT":{"block":{"type":"text","id":"text1","fields":{"TEXT":"legacy message"}}}}}]}}
            """);
        var document = validator.validateWorkspace(1,workspace,1L,params(),BlocklyValidationMode.ENABLE);
        template.setBlocklyJson(validator.write(document.getBlocklyJson()));
        var response = service.pushSync(request(Map.of()));
        assertThat(response.getChannelResults()).singleElement().satisfies(r -> {
            assertThat(r.getStatus()).isEqualTo(SendStatus.SUCCESS); assertThat(r.getMessageContent()).isEqualTo("legacy message");
        });
        verify(dependency(ChannelSenderDispatcher.class)).dispatch(eq("SMS"), any());
    }
    @Test void linearTimeFormatUsesSameCustomPatternDuringPreviewAndSend() {
        setLinearTimeTemplate("yyyy年MM月dd日");
        TemplatePreviewDTO preview = new TemplatePreviewDTO();
        preview.setTemplateId("10");
        preview.setValues(Map.of("p4", MAPPER.valueToTree("2026-11-12 11:12:13.123")));
        String previewContent = new com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl(
                dependency(MsgTemplateMapper.class),
                dependency(com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper.class),
                dependency(MsgSceneMapper.class), dependency(MsgSceneParamMapper.class), validator,
                new BlocklyRenderer(new SceneParamValueValidator())).preview(preview).getContent();

        var response = service.pushSync(request(Map.of("p4", "2026-11-12 11:12:13.123")));
        assertThat(response.getChannelResults()).singleElement().satisfies(result -> {
            assertThat(result.getMessageContent()).isEqualTo("2026年11月12日");
            assertThat(result.getMessageContent()).isEqualTo(previewContent);
            assertThat(result.getStatus()).isEqualTo(SendStatus.SUCCESS);
        });
    }
    @Test void objectFieldPathsReachRuleEngineThroughSendEntry() {
        ObjectNode rule = draft(); rule.withArray("lists").add(list("7",group("all"),content("{{n}}",binding("n","field","account.balance","NUMBER","money",2))));
        ((ObjectNode)rule.path("versions").get(0)).set("content",content("{{l}}",binding("l","list","list1","STRING","plain",0))); setRule(rule);
        var response = service.pushSync(request(Map.of("p1",0,"p7",List.of(Map.of("account",Map.of("balance","1.005"))))));
        assertThat(response.getChannelResults()).singleElement().satisfies(r -> assertThat(r.getMessageContent()).isEqualTo("[1.01]"));
    }
    @Test void malformedStoredDocumentUsesExistingFailureRecord() {
        template.setBlocklyJson("{broken");
        var response = service.pushSync(request(Map.of()));
        assertThat(response.getChannelResults()).singleElement().satisfies(r -> assertThat(r.getStatus()).isEqualTo(SendStatus.FAILED));
        verify(dependency(ChannelSenderDispatcher.class), never()).dispatch(anyString(), any());
        verify(dependency(MsgRecordMapper.class)).insert(any(MsgRecord.class));
    }
}
