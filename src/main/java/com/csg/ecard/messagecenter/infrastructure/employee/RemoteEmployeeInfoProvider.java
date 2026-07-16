package com.csg.ecard.messagecenter.infrastructure.employee;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内网员工中心实现，用于补齐接收人联系方式和所属组织。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.employee", name = "mode", havingValue = "remote")
public class RemoteEmployeeInfoProvider implements EmployeeInfoProvider {

    private final JadpUserClient jadpUserClient;
    private final EmployeeProperties employeeProperties;
    private final Cache<String, EmployeeInfo> employeeCache;

    public RemoteEmployeeInfoProvider(JadpUserClient jadpUserClient, EmployeeProperties employeeProperties) {
        this.jadpUserClient = jadpUserClient;
        this.employeeProperties = employeeProperties;
        EmployeeProperties.Remote remote = employeeProperties.getRemote();
        long timeoutMillis = cacheEnabled(remote) ? remote.getCacheTtl().toMillis() : 0L;
        this.employeeCache = CacheUtil.newLRUCache(Math.max(remote.getCacheMaxSize(), 1), timeoutMillis);
    }

    @Override
    public Map<String, EmployeeInfo> listUsers(Collection<String> userIds) {
        List<String> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return Map.of();
        }

        Map<String, EmployeeInfo> resolved = new LinkedHashMap<>();
        List<String> missingUserIds = new ArrayList<>();
        for (String userId : normalizedUserIds) {
            EmployeeInfo cached = cacheEnabled(employeeProperties.getRemote()) ? employeeCache.get(userId) : null;
            if (cached != null) {
                resolved.put(userId, cached);
            } else {
                missingUserIds.add(userId);
            }
        }

        if (!missingUserIds.isEmpty()) {
            try {
                List<RemoteUserDTO> response = jadpUserClient.getByUserIds(missingUserIds);
                if (response != null) {
                    for (RemoteUserDTO user : response) {
                        EmployeeInfo info = toEmployeeInfo(user);
                        if (info != null && StringUtils.hasText(info.userId())) {
                            resolved.put(info.userId(), info);
                            cacheEmployee(info);
                        }
                    }
                }
            } catch (FeignException ex) {
                log.warn("Employee remote query failed. userCount={}, status={}, cause={}",
                        missingUserIds.size(), ex.status(), ex.getMessage());
                throw new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR, "员工中心调用失败");
            }
        }

        Map<String, EmployeeInfo> result = new LinkedHashMap<>();
        for (String userId : normalizedUserIds) {
            EmployeeInfo info = resolved.get(userId);
            if (info != null) {
                result.put(userId, info);
            }
        }
        return result;
    }

    private void cacheEmployee(EmployeeInfo employeeInfo) {
        if (!cacheEnabled(employeeProperties.getRemote())) {
            return;
        }
        employeeCache.put(employeeInfo.userId(), employeeInfo);
    }

    private boolean cacheEnabled(EmployeeProperties.Remote remote) {
        return remote.getCacheTtl() != null
                && !remote.getCacheTtl().isZero()
                && !remote.getCacheTtl().isNegative();
    }

    @Override
    public List<String> getUnitPath(String unitId) {
        return StringUtils.hasText(unitId) ? List.of(unitId.trim()) : List.of();
    }

    private List<String> normalizeUserIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(userId -> !containsChinese(userId))
                .distinct()
                .toList();
    }

    private boolean containsChinese(String value) {
        return value.chars()
                .mapToObj(Character.UnicodeScript::of)
                .anyMatch(script -> script == Character.UnicodeScript.HAN);
    }

    private EmployeeInfo toEmployeeInfo(RemoteUserDTO user) {
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            return null;
        }
        return new EmployeeInfo(
                user.getUserId(),
                user.getEmployeeName(),
                user.getMobilePhone(),
                user.getEmail(),
                user.getOrgId(),
                null,
                user.getElinkUserId()
        );
    }

}
