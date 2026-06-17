package com.csg.ecard.messagecenter.module.scene.service;

import com.csg.ecard.messagecenter.module.scene.dto.SceneParamCreateDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamSortDTO;
import com.csg.ecard.messagecenter.module.scene.dto.SceneParamUpdateDTO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamVO;

import java.util.List;

/**
 * 场景参数管理服务。
 */
public interface SceneParamService {

    /**
     * 查询场景参数列表。
     *
     * @param sceneId 场景ID
     * @return 参数列表
     */
    List<SceneParamVO> list(Long sceneId);

    /**
     * 新增场景参数。
     *
     * @param sceneId 场景ID
     * @param request 新增请求
     * @return 新增后的参数
     */
    SceneParamVO create(Long sceneId, SceneParamCreateDTO request);

    /**
     * 编辑场景参数。
     *
     * @param sceneId 场景ID
     * @param paramId 参数ID
     * @param request 编辑请求
     * @return 编辑后的参数
     */
    SceneParamVO update(Long sceneId, Long paramId, SceneParamUpdateDTO request);

    /**
     * 删除场景参数。
     *
     * @param sceneId 场景ID
     * @param paramId 参数ID
     */
    void delete(Long sceneId, Long paramId);

    /**
     * 批量调整场景参数排序。
     *
     * @param sceneId 场景ID
     * @param request 排序请求
     */
    void sort(Long sceneId, SceneParamSortDTO request);

    /**
     * 查询场景参数引用情况。
     *
     * @param sceneId 场景ID
     * @param paramId 参数ID
     * @return 引用结果
     */
    SceneParamUsageVO usage(Long sceneId, Long paramId);
}
