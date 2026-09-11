package com.csg.ecard.messagecenter.module.template.blockly;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * 模板时间解析、格式校验和格式化工具。
 *
 * <p>线性模板与条件模板共用，未携带时区的值按 Asia/Shanghai 解释。</p>
 */
public final class TemplateTimeFormatter {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    public static final String BLOCKLY_DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String RULE_DEFAULT_PATTERN = "yyyy-MM-dd";
    public static final int MAX_PATTERN_LENGTH = 50;

    private static final DateTimeFormatter LOCAL_DATE_TIME_WITH_SECONDS = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter LOCAL_DATE_TIME_WITH_MINUTES =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private TemplateTimeFormatter() {
    }

    /** 校验并创建输出格式器。 */
    public static DateTimeFormatter outputFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern);
    }

    /**
     * 将模板时间值解析为时间点。
     *
     * <p>支持日期、本地日期时间、ISO 带时区日期时间，以及10位秒和13位毫秒时间戳。</p>
     */
    public static Instant parseInstant(String value) {
        if (value == null) {
            throw new DateTimeParseException("time is null", "", 0);
        }
        String text = value.trim();
        if (text.matches("-?\\d{10}")) {
            return Instant.ofEpochSecond(Long.parseLong(text));
        }
        if (text.matches("-?\\d{13}")) {
            return Instant.ofEpochMilli(Long.parseLong(text));
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // 继续尝试带偏移量的日期时间。
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // 继续尝试项目原有本地时间格式。
        }
        return parseLocalDateTime(text).atZone(DEFAULT_ZONE).toInstant();
    }

    /** 按指定格式和统一时区输出模板时间。 */
    public static String format(String value, String pattern) {
        DateTimeFormatter formatter = outputFormatter(pattern);
        return parseInstant(value).atZone(DEFAULT_ZONE).format(formatter);
    }

    /** 项目原有线性模板支持的本地时间解析。 */
    public static LocalDateTime parseLocalDateTime(String value) {
        for (DateTimeFormatter formatter : List.of(
                LOCAL_DATE_TIME_WITH_SECONDS,
                LOCAL_DATE_TIME_WITH_MINUTES)) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种兼容格式。
            }
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // 尝试 ISO_LOCAL_DATE_TIME。
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw ex;
        } catch (DateTimeException ex) {
            throw new DateTimeParseException("invalid time", value, 0, ex);
        }
    }
}
