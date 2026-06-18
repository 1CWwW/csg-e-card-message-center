package com.csg.ecard.messagecenter.module.template.service;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.mapper.TemplateQueryRow;
import com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息模板服务单元测试。
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

    @BeforeEach
    void setUp() {
        existed = new MsgTemplate();
        existed.setId(10L);
        existed.setSceneId(1L);
        existed.setChannelType(ChannelType.SMS.getCode());
        existed.setTemplateName("原模板");
        existed.setStatus(CommonStatus.DISABLE.getCode());
    }

    @Test
    void shouldUpdateChannelTypeWhenEditingTemplate() {
        TemplateUpdateDTO request = new TemplateUpdateDTO();
        request.setTemplateName("template");
        request.setChannelType(ChannelType.EMAIL.getCode());
        request.setStatus(CommonStatus.DISABLE.getCode());

        TemplateQueryRow detail = new TemplateQueryRow();
        detail.setId(existed.getId());
        detail.setTemplateName(request.getTemplateName());
        detail.setSceneId(existed.getSceneId());
        detail.setChannelType(ChannelType.EMAIL.getCode());
        detail.setStatus(CommonStatus.DISABLE.getCode());

        when(msgTemplateMapper.selectById(10L)).thenReturn(existed);
        when(msgTemplateMapper.selectCount(any())).thenReturn(0L);
        when(msgTemplateMapper.selectTemplateDetail(10L)).thenReturn(detail);

        msgTemplateService.update(10L, request);

        ArgumentCaptor<MsgTemplate> captor = ArgumentCaptor.forClass(MsgTemplate.class);
        verify(msgTemplateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
    }

    @Test
    void shouldRejectDuplicateTemplateNameInSameSceneWhenUpdating() {
        TemplateUpdateDTO request = new TemplateUpdateDTO();
        request.setChannelType(ChannelType.SMS.getCode());
        request.setTemplateName("同场景模板");
        request.setStatus(CommonStatus.DISABLE.getCode());

        when(msgTemplateMapper.selectById(10L)).thenReturn(existed);
        when(msgTemplateMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> msgTemplateService.update(10L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("当前场景下模板名称已存在")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_DUPLICATE.getCode());

        verify(msgTemplateMapper, never()).updateById(any(MsgTemplate.class));
        verify(msgTemplateUnitMapper, never()).deleteByTemplateId(any());
    }
}
