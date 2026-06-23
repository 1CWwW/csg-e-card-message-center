package com.csg.ecard.messagecenter.module.push.service.impl;

import com.csg.ecard.messagecenter.module.push.service.UnitPathResolver;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 当前阶段的默认单位路径解析器，仅返回用户当前单位。
 */
@Component
public class DefaultUnitPathResolver implements UnitPathResolver {

    @Override
    public List<String> resolve(String userOrgId) {
        return List.of(userOrgId);
    }
}
