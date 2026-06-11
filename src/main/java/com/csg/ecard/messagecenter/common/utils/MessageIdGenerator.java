package com.csg.ecard.messagecenter.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 消息 ID 生成工具。
 * <p>
 * 生成格式为 {@code MSG_yyyyMMdd_序列号}，序列号基于 Redis 按日期自增，适合多实例部署下生成趋势递增的消息编号。
 */
@Component
@RequiredArgsConstructor
public class MessageIdGenerator {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REDIS_KEY_PREFIX = "message:id:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.message-id.prefix:MSG}")
    private String prefix;

    /**
     * 生成下一个消息 ID。
     *
     * @return 消息 ID，例如 MSG_20260611_000001
     */
    public String nextId() {
        String day = LocalDate.now().format(DAY_FORMATTER);
        String key = REDIS_KEY_PREFIX + day;
        Long sequence = stringRedisTemplate.opsForValue().increment(key);
        if (sequence != null && sequence == 1L) {
            // 日期序列只需短期保留，设置 2 天过期可覆盖跨日与时钟边界场景。
            stringRedisTemplate.expire(key, Duration.ofDays(2));
        }
        return prefix + "_" + day + "_" + String.format("%06d", sequence == null ? 1L : sequence);
    }
}
