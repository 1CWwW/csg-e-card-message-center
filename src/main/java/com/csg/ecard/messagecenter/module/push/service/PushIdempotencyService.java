package com.csg.ecard.messagecenter.module.push.service;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import com.csg.ecard.messagecenter.common.utils.RedisUtil;
import com.csg.ecard.messagecenter.module.push.vo.SyncPushVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同步推送幂等服务，在幂等窗口内保存业务ID对应的消息ID和最终响应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushIdempotencyService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final Duration CONSUME_TTL = Duration.ofDays(1);
    private static final String KEY_PREFIX = MessageCenterConstants.IDEMPOTENT_KEY_PREFIX + "push:biz:";
    private static final String CONSUME_KEY_PREFIX = MessageCenterConstants.IDEMPOTENT_KEY_PREFIX + "push:consume:";
    private static final String RESULT_SUFFIX = ":result";
    private static final Map<String, LocalValue> LOCAL_VALUES = new ConcurrentHashMap<>();

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    /**
     * 尝试为业务ID绑定消息ID。
     *
     * @param bizId          业务ID
     * @param candidateMsgId 当前请求生成的消息ID
     * @return 幂等占位结果
     */
    public AcquireResult acquire(String bizId, String candidateMsgId) {
        String key = key(bizId);
        try {
            if (Boolean.TRUE.equals(redisUtil.setIfAbsent(key, candidateMsgId, DEFAULT_TTL))) {
                return new AcquireResult(true, candidateMsgId);
            }
            Object existing = redisUtil.get(key);
            if (existing != null) {
                return new AcquireResult(false, existing.toString());
            }
            if (Boolean.TRUE.equals(redisUtil.setIfAbsent(key, candidateMsgId, DEFAULT_TTL))) {
                return new AcquireResult(true, candidateMsgId);
            }
            Object raced = redisUtil.get(key);
            return new AcquireResult(false, raced == null ? candidateMsgId : raced.toString());
        } catch (RuntimeException ex) {
            log.warn("Redis push idempotency unavailable, fallback to local key. key={}, cause={}",
                    key, ex.getMessage());
            return acquireLocal(key, candidateMsgId);
        }
    }

    /**
     * 缓存已完成的同步推送响应。
     */
    public void saveResult(String bizId, SyncPushVO result) {
        String resultKey = key(bizId) + RESULT_SUFFIX;
        try {
            redisUtil.set(resultKey, result, DEFAULT_TTL);
        } catch (RuntimeException ex) {
            log.warn("Redis push result cache unavailable, fallback to local result. key={}, cause={}",
                    resultKey, ex.getMessage());
            LOCAL_VALUES.put(resultKey, new LocalValue(result, Instant.now().plus(DEFAULT_TTL)));
        }
    }

    /**
     * 读取已完成的同步推送响应。
     */
    public SyncPushVO getResult(String bizId) {
        String resultKey = key(bizId) + RESULT_SUFFIX;
        try {
            Object value = redisUtil.get(resultKey);
            return value == null ? null : objectMapper.convertValue(value, SyncPushVO.class);
        } catch (RuntimeException ex) {
            log.warn("Redis push result read unavailable, fallback to local result. key={}, cause={}",
                    resultKey, ex.getMessage());
            return localValue(resultKey, SyncPushVO.class);
        }
    }

    /**
     * 在事务回滚时释放本次幂等占位。
     */
    public void release(String bizId) {
        String key = key(bizId);
        try {
            redisUtil.delete(key);
            redisUtil.delete(key + RESULT_SUFFIX);
        } catch (RuntimeException ex) {
            log.warn("Redis push idempotency release unavailable, fallback to local release. key={}, cause={}",
                    key, ex.getMessage());
        } finally {
            LOCAL_VALUES.remove(key);
            LOCAL_VALUES.remove(key + RESULT_SUFFIX);
        }
    }

    /**
     * 尝试获取指定消息消费轮次的幂等占位。
     *
     * @param msgId      消息ID
     * @param retryCount 当前重试次数
     * @return true表示本轮首次消费
     */
    public boolean acquireConsumption(String msgId, int retryCount) {
        String key = CONSUME_KEY_PREFIX + msgId + ":" + retryCount;
        try {
            return Boolean.TRUE.equals(redisUtil.setIfAbsent(key, "1", CONSUME_TTL));
        } catch (RuntimeException ex) {
            log.warn("Redis consume idempotency unavailable, fallback to local key. key={}, cause={}",
                    key, ex.getMessage());
            return acquireLocal(key, "1", CONSUME_TTL).acquired();
        }
    }

    private AcquireResult acquireLocal(String key, String candidateMsgId) {
        return acquireLocal(key, candidateMsgId, DEFAULT_TTL);
    }

    private AcquireResult acquireLocal(String key, String candidateMsgId, Duration ttl) {
        cleanupExpired();
        LocalValue candidate = new LocalValue(candidateMsgId, Instant.now().plus(ttl));
        LocalValue existing = LOCAL_VALUES.putIfAbsent(key, candidate);
        return existing == null
                ? new AcquireResult(true, candidateMsgId)
                : new AcquireResult(false, existing.value().toString());
    }

    private <T> T localValue(String key, Class<T> type) {
        cleanupExpired();
        LocalValue value = LOCAL_VALUES.get(key);
        return value == null ? null : objectMapper.convertValue(value.value(), type);
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        LOCAL_VALUES.entrySet().removeIf(entry -> !entry.getValue().expireAt().isAfter(now));
    }

    private String key(String bizId) {
        return KEY_PREFIX + bizId;
    }

    public record AcquireResult(boolean acquired, String msgId) {
    }

    private record LocalValue(Object value, Instant expireAt) {
    }
}
