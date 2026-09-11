package com.csg.ecard.messagecenter.module.template.service;

import com.csg.ecard.messagecenter.module.scene.entity.MsgScene;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.blockly.SceneParamValueValidator;
import com.csg.ecard.messagecenter.module.template.dto.TemplateContentSaveDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.csg.ecard.messagecenter.module.template.rule.RuleFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 条件模板沿用原内容保存接口和启停语义。 */
class RuleTemplateSaveTest {

    @Test
    void ruleTemplateUsesOriginalContentColumnAndDisablesChangedEnabledTemplate() {
        MsgTemplateMapper templates = mock(MsgTemplateMapper.class);
        MsgTemplateUnitMapper units = mock(MsgTemplateUnitMapper.class);
        MsgSceneMapper scenes = mock(MsgSceneMapper.class);
        MsgSceneParamMapper parameters = mock(MsgSceneParamMapper.class);
        BlocklyJsonValidator validator = new BlocklyJsonValidator(MAPPER);
        MsgTemplateServiceImpl service = new MsgTemplateServiceImpl(
                templates, units, scenes, parameters, validator,
                new BlocklyRenderer(new SceneParamValueValidator()));

        MsgTemplate initial = new MsgTemplate();
        initial.setId(10L);
        initial.setSceneId(1L);
        initial.setChannelType("SMS");
        initial.setStatus(1);
        AtomicReference<MsgTemplate> state = new AtomicReference<>(initial);
        when(templates.selectById(10L)).thenAnswer(ignored -> state.get());
        when(templates.updateById(any(MsgTemplate.class))).thenAnswer(invocation -> {
            MsgTemplate update = invocation.getArgument(0);
            state.get().setBlocklyJson(update.getBlocklyJson());
            if (update.getStatus() != null) {
                state.get().setStatus(update.getStatus());
            }
            return 1;
        });
        MsgScene scene = new MsgScene();
        scene.setId(1L);
        scene.setStatus(1);
        when(scenes.selectById(1L)).thenReturn(scene);
        when(parameters.selectList(any())).thenReturn(new ArrayList<>(params().values()));

        ObjectNode rule = draft();
        ObjectNode amount = binding("amount", "param", "1", "NUMBER", "money", 2);
        amount.putArray("calculations")
                .add(calculation("subtract", "MINUS", "100").put("currentSide", "right"));
        ((ObjectNode) rule.path("versions").get(0))
                .set("content", content("{{amount}}", amount));
        ((ObjectNode) rule.path("versions").get(0).path("condition").path("rules").get(0))
                .put("negate", true);
        ObjectNode workspace = MAPPER.createObjectNode();
        workspace.set("ruleTemplate", rule.deepCopy());
        workspace.putObject("blocks").put("languageVersion", 0).putArray("blocks");
        workspace.putObject("layout").put("x", 120).put("y", 240);

        TemplateContentSaveDTO request = new TemplateContentSaveDTO();
        request.setEditorType("RULE_VERSIONS");
        request.setSchemaVersion(1);
        request.setRuleTemplate(rule);
        request.setWorkspace(workspace);

        var result = service.saveContent(10L, request);

        assertThat(result.getValid()).isTrue();
        assertThat(result.getBlocklyJson().path("editorType").asText()).isEqualTo("RULE_VERSIONS");
        assertThat(result.getBlocklyJson().path("ruleTemplate")).isEqualTo(rule);
        assertThat(result.getBlocklyJson().path("workspace")).isEqualTo(workspace);
        assertThat(state.get().getStatus()).isZero();
        verify(templates).updateById(any(MsgTemplate.class));
    }
}
