package com.csg.ecard.messagecenter.infrastructure.employee;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 默认员工信息提供实现，等待员工中心接入后替换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.employee", name = "mode", havingValue = "preset", matchIfMissing = true)
public class DefaultEmployeeInfoProvider implements EmployeeInfoProvider {

    private final LocalUnitPathProperties localUnitPathProperties;

    @Override
    public Map<String, EmployeeInfo> listUsers(Collection<String> userIds) {
        return Map.of();
    }

    @Override
    public List<String> getUnitPath(String unitId) {
        if (!StringUtils.hasText(unitId)) {
            return List.of();
        }
        List<String> path = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        String current = unitId.trim();
        int maxDepth = Math.max(localUnitPathProperties.getMaxDepth(), 1);
        for (int depth = 0; StringUtils.hasText(current) && depth < maxDepth; depth++) {
            String normalized = current.trim();
            if (!visited.add(normalized)) {
                log.warn("本地单位层级配置存在循环，unitId={}, path={}", normalized, path);
                break;
            }
            path.add(normalized);
            current = localUnitPathProperties.getParentByUnit().get(normalized);
        }
        return path;
    }
}
