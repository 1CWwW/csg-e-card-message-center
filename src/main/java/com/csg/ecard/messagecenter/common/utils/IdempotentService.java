package com.csg.ecard.messagecenter.common.utils;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等基础服务。
 * <p>
 * 优先使用 Redis {@code SET NX EX} 实现分布式幂等占位；Redis 不可用时降级为本地内存占位，
 * 仅保证当前 JVM 内短期防重复，生产环境建议保障 Redis 可用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentService {

    private static final Map<String, Instant> LOCAL_KEYS = new ConcurrentHashMap<>();

    private final RedisUtil redisUtil;

    @Value("${app.idempotent.key-prefix:" + MessageCenterConstants.IDEMPOTENT_KEY_PREFIX + "}")
    private String keyPrefix;

    @Value("${app.idempotent.default-ttl-seconds:300}")
    private long defaultTtlSeconds;

    /**
     * 使用默认 TTL 尝试获取幂等占位。
     *
     * @param bizKey 业务幂等 key
     * @return true 表示首次请求，false 表示重复请求
     */
    public boolean tryAcquire(String bizKey) {
        return tryAcquire(bizKey, Duration.ofSeconds(defaultTtlSeconds));
    }

    /**
     * 使用指定 TTL 尝试获取幂等占位。
     *
     * @param bizKey 业务幂等 key
     * @param ttl    占位过期时间
     * @return true 表示占位成功，false 表示 key 已存在
     */
    public boolean tryAcquire(String bizKey, Duration ttl) {
        if (!StringUtils.hasText(bizKey)) {
            return false;
        }
        String key = keyPrefix + bizKey;
        try {
            Boolean acquired = redisUtil.setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (RuntimeException ex) {
            log.warn("Redis idempotent key unavailable, fallback to local key. key={}, cause={}",
                    key, ex.getMessage());
            return tryAcquireLocal(key, ttl);
        }
    }

    /**
     * 根据业务 ID 判断是否重复请求。
     *
     * @param bizId 业务 ID
     * @return true 表示重复，false 表示首次
     */
    public boolean isRepeatedByBizId(String bizId) {
        return isRepeatedByBizId(bizId, Duration.ofSeconds(defaultTtlSeconds));
    }

    /**
     * 根据业务 ID 判断是否重复请求。
     *
     * @param bizId 业务 ID
     * @param ttl   占位过期时间
     * @return true 表示重复，false 表示首次
     */
    public boolean isRepeatedByBizId(String bizId, Duration ttl) {
        return !tryAcquire("biz:" + bizId, ttl);
    }

    /**
     * 根据请求 ID 判断是否重复请求。
     *
     * @param requestId 请求 ID
     * @return true 表示重复，false 表示首次
     */
    public boolean isRepeatedByRequestId(String requestId) {
        return isRepeatedByRequestId(requestId, Duration.ofSeconds(defaultTtlSeconds));
    }

    /**
     * 根据请求 ID 判断是否重复请求。
     *
     * @param requestId 请求 ID
     * @param ttl       占位过期时间
     * @return true 表示重复，false 表示首次
     */
    public boolean isRepeatedByRequestId(String requestId, Duration ttl) {
        return !tryAcquire("request:" + requestId, ttl);
    }

    /**
     * 释放幂等占位。
     *
     * @param bizKey 业务幂等 key
     */
    public void release(String bizKey) {
        if (!StringUtils.hasText(bizKey)) {
            return;
        }
        String key = keyPrefix + bizKey;
        try {
            redisUtil.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Redis idempotent release unavailable, fallback to local release. key={}, cause={}",
                    key, ex.getMessage());
            LOCAL_KEYS.remove(key);
        }
    }

    private boolean tryAcquireLocal(String key, Duration ttl) {
        cleanupExpiredLocalKeys();
        Instant expireAt = Instant.now().plus(ttl == null ? Duration.ofSeconds(defaultTtlSeconds) : ttl);
        return LOCAL_KEYS.putIfAbsent(key, expireAt) == null;
    }

    private void cleanupExpiredLocalKeys() {
        Instant now = Instant.now();
        LOCAL_KEYS.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
