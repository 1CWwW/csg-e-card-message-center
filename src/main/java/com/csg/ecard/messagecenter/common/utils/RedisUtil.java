package com.csg.ecard.messagecenter.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作工具类。
 * <p>
 * 封装基础字符串键值、过期时间、删除、自增和 set-if-absent 操作，供基础框架和后续业务能力复用。
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入永久缓存。
     *
     * @param key   Redis key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
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
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 读取缓存值。
     *
     * @param key Redis key
     * @return 缓存值；不存在时返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个 key。
     *
     * @param key Redis key
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除 key。
     *
     * @param keys Redis key 集合
     * @return 删除数量
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断 key 是否存在。
     *
     * @param key Redis key
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
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
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 对整数值执行自增。
     *
     * @param key Redis key
     * @return 自增后的值
     */
    public Long increment(String key) {
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
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }
}
