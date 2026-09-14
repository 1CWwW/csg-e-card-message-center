package com.csg.ecard.messagecenter.module.dnd.service;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbBatchCreateDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbPageQueryDTO;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbUpdateDTO;
import com.csg.ecard.messagecenter.module.dnd.vo.DoNotDisturbRuleVO;

import java.util.List;

/**
 * 免打扰规则管理服务。
 */
public interface DoNotDisturbRuleService {

    PageResult<DoNotDisturbRuleVO> page(DoNotDisturbPageQueryDTO query);

    DoNotDisturbRuleVO detail(Long id);

    List<DoNotDisturbRuleVO> createBatch(DoNotDisturbBatchCreateDTO request);

    DoNotDisturbRuleVO update(Long id, DoNotDisturbUpdateDTO request);

    void delete(Long id);

    DoNotDisturbRuleVO toggle(Long id);
}
