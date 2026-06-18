package com.csg.ecard.messagecenter.module.scene.usage;

import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;

/**
 * 场景参数引用检查扩展点。
 */
public interface SceneParamUsageChecker {

    /**
     * 查询参数引用情况。
     *
     * @param param 场景参数
     * @return 引用结果
     */
    SceneParamUsageVO checkUsage(MsgSceneParam param);

    /**
     * 一次构建指定场景全部参数的模板引用索引。
     *
     * @param sceneId 场景ID
     * @return 场景参数引用索引
     */
    SceneParamUsageIndex buildUsageIndex(Long sceneId);
}
