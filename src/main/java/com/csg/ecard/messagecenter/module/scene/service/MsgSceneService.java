package com.csg.ecard.messagecenter.module.scene.service;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.scene.dto.SceneCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.ScenePageQueryDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.vo.MsgSceneVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneCodeCheckVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneDisableCheckVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneOverviewVO;

/**
 * 场景管理服务。
 */
public interface MsgSceneService {

    /**
     * 查询场景概览。
     *
     * @return 场景概览
     */
    SceneOverviewVO overview();

    /**
     * 分页查询场景。
     *
     * @param query 查询条件
     * @return 场景分页结果
     */
    PageResult<MsgSceneVO> page(ScenePageQueryDTO query);

    /**
     * 查询场景详情。
     *
     * @param id 场景ID
     * @return 场景详情
     */
    MsgSceneVO detail(Long id);

    /**
     * 检查场景停用影响。
     *
     * @param id 场景ID
     * @return 停用检查结果
     */
    SceneDisableCheckVO disableCheck(Long id);

    /**
     * 检查场景编码是否可用。
     *
     * @param sceneCode 场景编码
     * @return 编码可用性
     */
    SceneCodeCheckVO checkCode(String sceneCode, Long excludeId);

    /**
     * 新增场景。
     *
     * @param request 新增请求
     * @return 新增后的场景
     */
    MsgSceneVO create(SceneCreateDTO request);

    /**
     * 编辑场景。
     *
     * @param id 场景ID
     * @param request 编辑请求
     * @return 编辑后的场景
     */
    MsgSceneVO update(Long id, SceneUpdateDTO request);

    /**
     * 删除场景。
     *
     * @param id 场景ID
     */
    void delete(Long id);

    /**
     * 启停切换场景。
     *
     * @param id 场景ID
     * @return 切换后的场景
     */
    MsgSceneVO toggle(Long id);
}
