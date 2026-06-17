package com.csg.ecard.messagecenter.module.scene.usage;

import com.csg.ecard.messagecenter.module.scene.entity.MsgSceneParam;
import com.csg.ecard.messagecenter.module.scene.vo.SceneParamUsageVO;
import org.springframework.stereotype.Component;

/**
 * 默认场景参数引用检查器。
 * <p>
 * 模板和 Blockly 模块尚未实现，当前统一返回未引用；后续替换该实现即可接入真实引用检查。
 */
@Component
public class DefaultSceneParamUsageChecker implements SceneParamUsageChecker {

    @Override
    public SceneParamUsageVO checkUsage(MsgSceneParam param) {
        return SceneParamUsageVO.unused();
    }
}
