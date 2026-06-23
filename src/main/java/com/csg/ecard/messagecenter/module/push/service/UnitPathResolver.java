package com.csg.ecard.messagecenter.module.push.service;

import java.util.List;

/**
 * 用户单位路径解析器。
 */
public interface UnitPathResolver {

    /**
     * 解析从当前单位到上级单位的匹配路径。
     *
     * @param userOrgId 用户当前单位ID
     * @return 单位路径
     */
    List<String> resolve(String userOrgId);
}
