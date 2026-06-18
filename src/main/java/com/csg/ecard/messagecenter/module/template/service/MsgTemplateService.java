package com.csg.ecard.messagecenter.module.template.service;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCopyDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateContentSaveDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateCreateDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplatePreviewDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateReferencePageQueryDTO;
import com.csg.ecard.messagecenter.module.template.dto.TemplateUpdateDTO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateCopyVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateContentVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplatePreviewVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceDetailVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceListVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateReferenceVO;
import com.csg.ecard.messagecenter.module.template.vo.TemplateToolboxVO;

import java.util.List;

/**
 * 消息模板基础管理服务。
 */
public interface MsgTemplateService {

    PageResult<TemplateListVO> page(TemplatePageQueryDTO query);

    TemplateDetailVO detail(Long id);

    TemplateDetailVO create(TemplateCreateDTO request);

    TemplateDetailVO update(Long id, TemplateUpdateDTO request);

    void delete(Long id);

    TemplateDetailVO toggle(Long id);

    TemplateCopyVO copy(Long id, TemplateCopyDTO request);

    TemplateContentVO saveContent(Long id, TemplateContentSaveDTO request);

    TemplatePreviewVO preview(TemplatePreviewDTO request);

    TemplateToolboxVO toolbox(Long id);

    List<TemplateReferenceVO> references(Long id);

    TemplateReferenceDetailVO referenceDetail(Long id, Long referenceId);

    PageResult<TemplateReferenceListVO> referencePage(TemplateReferencePageQueryDTO query);
}
