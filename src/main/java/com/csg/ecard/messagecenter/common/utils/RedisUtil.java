package com.csg.ecard.messagecenter.common.utils;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 常用操作工具类。
 * <p>
 * 封装基础字符串键值、过期时间、删除、自增和 set-if-absent 操作，供基础框架和后续业务能力复用。
 */
@Component
public class RedisUtil {

    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;
    private final Map<String, LocalValue> localValues = new ConcurrentHashMap<>();

    @Value("${app.redis.enabled:true}")
    private boolean redisEnabled;

    public RedisUtil(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 写入永久缓存。
     *
     * @param key   Redis key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            localValues.put(key, new LocalValue(value, null));
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入带过期时间的缓存。
     *
     * @param key     Redis key
     * @param value   缓存值
     * @param timeout 过期时间
     */
    public void set(String key, Object value, Duration timeout) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            localValues.put(key, new LocalValue(value, expireAt(timeout)));
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 读取缓存值。
     *
     * @param key Redis key
     * @return 缓存值；不存在时返回 null
     */
    public Object get(String key) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return localGet(key);
        }
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个 key。
     *
     * @param key Redis key
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return localValues.remove(key) != null;
        }
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除 key。
     *
     * @param keys Redis key 集合
     * @return 删除数量
     */
    public Long delete(Collection<String> keys) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            long deleted = 0;
            for (String key : keys) {
                if (localValues.remove(key) != null) {
                    deleted++;
                }
            }
            return deleted;
        }
        return redisTemplate.delete(keys);
    }

    /**
     * 判断 key 是否存在。
     *
     * @param key Redis key
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return localGet(key) != null;
        }
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置 key 的过期时间。
     *
     * @param key     Redis key
     * @param timeout 过期时长
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            LocalValue value = localValues.get(key);
            if (value == null || value.expired()) {
                localValues.remove(key);
                return false;
            }
            localValues.put(key, new LocalValue(value.value(), Instant.now().plusMillis(unit.toMillis(timeout))));
            return true;
        }
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 对整数值执行自增。
     *
     * @param key Redis key
     * @return 自增后的值
     */
    public Long increment(String key) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            cleanupExpiredLocalValues();
            LocalValue updated = localValues.compute(key, (ignored, existing) -> {
                long current = 0;
                if (existing != null && !existing.expired()) {
                    Object value = existing.value();
                    current = value instanceof AtomicLong atomic ? atomic.get()
                            : value instanceof Number number ? number.longValue()
                            : Long.parseLong(String.valueOf(value));
                }
                return new LocalValue(new AtomicLong(current + 1), null);
            });
            return ((AtomicLong) updated.value()).get();
        }
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 仅当 key 不存在时写入值，常用于幂等锁或轻量分布式锁。
     *
     * @param key     Redis key
     * @param value   写入值
     * @param timeout 过期时间，必须设置以避免异常场景下锁永久残留
     * @return true 表示写入成功，false 表示 key 已存在
     */
    public Boolean setIfAbsent(String key, Object value, Duration timeout) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            cleanupExpiredLocalValues();
            return localValues.putIfAbsent(key, new LocalValue(value, expireAt(timeout))) == null;
        }
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    private RedisTemplate<String, Object> redisTemplate() {
        if (!redisEnabled) {
            return null;
        }
        return redisTemplateProvider.getIfAvailable();
    }

    private Object localGet(String key) {
        LocalValue value = localValues.get(key);
        if (value == null) {
            return null;
        }
        if (value.expired()) {
            localValues.remove(key);
            return null;
        }
        Object raw = value.value();
        return raw instanceof AtomicLong atomic ? atomic.get() : raw;
    }

    private Instant expireAt(Duration timeout) {
        return timeout == null ? null : Instant.now().plus(timeout);
    }

    private void cleanupExpiredLocalValues() {
        localValues.entrySet().removeIf(entry -> entry.getValue().expired());
    }

    private record LocalValue(Object value, Instant expireAt) {

        private boolean expired() {
            return expireAt != null && !expireAt.isAfter(Instant.now());
        }
    }
}
