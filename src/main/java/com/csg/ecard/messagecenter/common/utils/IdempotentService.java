package com.csg.ecard.messagecenter.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 幂等占位服务。
 * <p>
 * 基于 Redis {@code SET NX EX} 语义实现一次性占位，适用于提交防重复、消息消费幂等等场景。
 * 当前仅提供基础能力，不绑定具体业务模块。
 */
@Component
@RequiredArgsConstructor
public class IdempotentService {

    private final RedisUtil redisUtil;

    @Value("${app.idempotent.key-prefix:idem:}")
    private String keyPrefix;

    @Value("${app.idempotent.default-ttl-seconds:300}")
    private long defaultTtlSeconds;

    /**
     * 使用默认 TTL 尝试获取幂等占位。
     *
     * @param bizKey 业务幂等 key，调用方应保证能唯一标识一次业务请求
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
        Boolean acquired = redisUtil.setIfAbsent(keyPrefix + bizKey, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放幂等占位。
     * <p>
     * 仅建议在业务明确失败且允许重试时调用；成功请求通常等待 TTL 自然过期。
     *
     * @param bizKey 业务幂等 key
     */
    public void release(String bizKey) {
        redisUtil.delete(keyPrefix + bizKey);
    }
}
