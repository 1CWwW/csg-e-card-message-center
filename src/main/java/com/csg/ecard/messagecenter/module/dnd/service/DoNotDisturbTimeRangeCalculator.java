package com.csg.ecard.messagecenter.module.dnd.service;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.dnd.dto.DoNotDisturbTimeRangeDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 免打扰时间段校验与下一可发送时间计算器。
 */
@Component
public class DoNotDisturbTimeRangeCalculator {

    private static final long NANOS_PER_DAY = 86_400_000_000_000L;

    /**
     * 校验时间段的完整性、重叠和全天覆盖情况。
     *
     * @param ranges 时间段
     * @return 可安全保存的时间段副本
     */
    public List<DoNotDisturbTimeRangeDTO> validateAndCopy(List<DoNotDisturbTimeRangeDTO> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "免打扰时间段不能为空");
        }
        List<DoNotDisturbTimeRangeDTO> copies = new ArrayList<>(ranges.size());
        List<TimeSegment> segments = new ArrayList<>();
        for (DoNotDisturbTimeRangeDTO range : ranges) {
            if (range == null || range.getStartTime() == null || range.getEndTime() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "免打扰开始时间和结束时间不能为空");
            }
            LocalTime start = range.getStartTime();
            LocalTime end = range.getEndTime();
            if (start.equals(end)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "免打扰开始时间和结束时间不能相同");
            }
            DoNotDisturbTimeRangeDTO copy = new DoNotDisturbTimeRangeDTO();
            copy.setStartTime(start);
            copy.setEndTime(end);
            copies.add(copy);

            long startNano = start.toNanoOfDay();
            long endNano = end.toNanoOfDay();
            if (startNano < endNano) {
                segments.add(new TimeSegment(startNano, endNano));
            } else {
                segments.add(new TimeSegment(startNano, NANOS_PER_DAY));
                if (endNano > 0) {
                    segments.add(new TimeSegment(0, endNano));
                }
            }
        }
        validateSegments(segments);
        return List.copyOf(copies);
    }

    /**
     * 计算不落在免打扰区间内的最早时间。
     *
     * @param candidate 候选发送时间
     * @param ranges    已校验时间段
     * @return 最早允许发送时间
     */
    public LocalDateTime nextAllowed(LocalDateTime candidate, List<DoNotDisturbTimeRangeDTO> ranges) {
        LocalDateTime current = candidate;
        for (int index = 0; index <= ranges.size(); index++) {
            DoNotDisturbTimeRangeDTO matched = findMatched(current.toLocalTime(), ranges);
            if (matched == null) {
                return current;
            }
            LocalTime start = matched.getStartTime();
            LocalTime end = matched.getEndTime();
            if (start.isBefore(end)) {
                current = current.toLocalDate().atTime(end);
            } else if (!current.toLocalTime().isBefore(start)) {
                current = current.toLocalDate().plusDays(1).atTime(end);
            } else {
                current = current.toLocalDate().atTime(end);
            }
        }
        throw new BizException(ErrorCode.BUSINESS_ERROR, "免打扰规则无法计算可发送时间");
    }

    private DoNotDisturbTimeRangeDTO findMatched(LocalTime time, List<DoNotDisturbTimeRangeDTO> ranges) {
        for (DoNotDisturbTimeRangeDTO range : ranges) {
            LocalTime start = range.getStartTime();
            LocalTime end = range.getEndTime();
            boolean matched = start.isBefore(end)
                    ? !time.isBefore(start) && time.isBefore(end)
                    : !time.isBefore(start) || time.isBefore(end);
            if (matched) {
                return range;
            }
        }
        return null;
    }

    private void validateSegments(List<TimeSegment> segments) {
        segments.sort(Comparator.comparingLong(TimeSegment::start));
        long coveredNanos = 0;
        long previousEnd = -1;
        for (TimeSegment segment : segments) {
            if (segment.start() < previousEnd) {
                throw new BizException(ErrorCode.PARAM_ERROR, "免打扰时间段不能重叠");
            }
            coveredNanos += segment.end() - segment.start();
            previousEnd = segment.end();
        }
        if (coveredNanos >= NANOS_PER_DAY) {
            throw new BizException(ErrorCode.PARAM_ERROR, "免打扰时间段不能覆盖全天");
        }
    }

    private record TimeSegment(long start, long end) {
    }
}
