package com.csg.ecard.messagecenter.common.utils;

import com.csg.ecard.messagecenter.common.constant.MessageCenterConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息 ID 生成工具。
 * <p>
 * 生成格式为 {@code MSG_yyyyMMdd_序列号}。生产环境建议使用 Redis 自增保证多实例下全局唯一；
 * Redis 暂时不可用时降级为本地内存序列，仅保证当前 JVM 内并发安全。
 */
@Slf4j
@Component
public class MessageIdGenerator {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SEQUENCE_WIDTH = 5;
    private static final AtomicLong LOCAL_SEQUENCE = new AtomicLong();

    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;

    @Value("${app.redis.enabled:true}")
    private boolean redisEnabled;

    private volatile String localSequenceDay;

    @Autowired
    public MessageIdGenerator(ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    public MessageIdGenerator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplateProvider = new SingletonObjectProvider(stringRedisTemplate);
    }

    /**
     * 生成下一个消息 ID。
     *
     * @return 消息 ID，例如 MSG_20260527_00001
     */
    public String nextId() {
        String day = LocalDate.now().format(DAY_FORMATTER);
        long sequence = nextSequence(day);
        return MessageCenterConstants.MESSAGE_ID_PREFIX + day + "_" + String.format("%0" + SEQUENCE_WIDTH + "d", sequence);
    }

    private long nextSequence(String day) {
        String key = MessageCenterConstants.MESSAGE_ID_PREFIX + "seq:" + day;
        if (!redisEnabled) {
            return nextLocalSequence(day);
        }
        try {
            StringRedisTemplate stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
            if (stringRedisTemplate == null) {
                return nextLocalSequence(day);
            }
            Long sequence = stringRedisTemplate.opsForValue().increment(key);
            if (sequence != null && sequence == 1L) {
                stringRedisTemplate.expire(key, Duration.ofDays(2));
            }
            return sequence == null ? nextLocalSequence(day) : sequence;
        } catch (RuntimeException ex) {
            log.warn("Redis message id sequence unavailable, fallback to local sequence. day={}, cause={}",
                    day, ex.getMessage());
            return nextLocalSequence(day);
        }
    }

    private long nextLocalSequence(String day) {
        if (!day.equals(localSequenceDay)) {
            synchronized (LOCAL_SEQUENCE) {
                if (!day.equals(localSequenceDay)) {
                    LOCAL_SEQUENCE.set(0);
                    localSequenceDay = day;
                }
            }
        }
        return LOCAL_SEQUENCE.incrementAndGet();
    }

    private record SingletonObjectProvider(StringRedisTemplate value) implements ObjectProvider<StringRedisTemplate> {

        @Override
        public StringRedisTemplate getObject(Object... args) {
            return value;
        }

        @Override
        public StringRedisTemplate getIfAvailable() {
            return value;
        }

        @Override
        public StringRedisTemplate getIfUnique() {
            return value;
        }

        @Override
        public StringRedisTemplate getObject() {
            return value;
        }

        @Override
        public Iterator<StringRedisTemplate> iterator() {
            return value == null ? Collections.emptyIterator() : java.util.List.of(value).iterator();
        }
    }
}
