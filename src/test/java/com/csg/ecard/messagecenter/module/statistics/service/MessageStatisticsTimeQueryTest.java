package com.csg.ecard.messagecenter.module.statistics.service;

import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsTimeQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.mapper.MessageStatisticsMapper;
import com.csg.ecard.messagecenter.module.statistics.mapper.MessageStatisticsQueryCriteria;
import com.csg.ecard.messagecenter.module.statistics.mapper.MessageStatisticsSqlProvider;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsCountRow;
import com.csg.ecard.messagecenter.module.statistics.mapper.StatisticsTimeRow;
import com.csg.ecard.messagecenter.module.statistics.service.impl.MessageStatisticsServiceImpl;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 指定模板按日发送统计测试。
 */
@ExtendWith(MockitoExtension.class)
class MessageStatisticsTimeQueryTest {

    private static final Long TEMPLATE_ID = 2085629226374467586L;

    @Mock
    private MessageStatisticsMapper messageStatisticsMapper;
    @Mock
    private OrganizationProvider organizationProvider;

    @InjectMocks
    private MessageStatisticsServiceImpl messageStatisticsService;

    @Test
    void shouldReturnDailyCountsForOnlyRequestedTemplatesAndTimeRange() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 29, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
        when(messageStatisticsMapper.selectTimeDay(any()))
                .thenReturn(List.of(timeRow("2026-08-29", 2, 1, 1),
                        timeRow("2026-08-31", 1, 1, 0)));
        when(messageStatisticsMapper.selectSummary(any()))
                .thenReturn(countRow(3, 2, 1));
        MessageStatisticsTimeQueryDTO query = new MessageStatisticsTimeQueryDTO();
        query.setTemplateIds(List.of(TEMPLATE_ID));
        query.setGranularity("DAY");
        query.setStartTime(startTime);
        query.setEndTime(endTime);

        var result = messageStatisticsService.time(query);

        assertThat(result.getGranularity()).isEqualTo("DAY");
        assertThat(result.getItems())
                .extracting("period", "periodLabel", "totalCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("2026-08-29", "2026-08-29", 2L),
                        org.assertj.core.groups.Tuple.tuple("2026-08-30", "2026-08-30", 0L),
                        org.assertj.core.groups.Tuple.tuple("2026-08-31", "2026-08-31", 1L));
        assertThat(result.getSummary().getTotalCount()).isEqualTo(3L);

        ArgumentCaptor<MessageStatisticsQueryCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(MessageStatisticsQueryCriteria.class);
        verify(messageStatisticsMapper).selectTimeDay(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().getTemplateIds()).containsExactly(TEMPLATE_ID);
        assertThat(criteriaCaptor.getValue().getStartTime()).isEqualTo(startTime);
        assertThat(criteriaCaptor.getValue().getEndTime()).isEqualTo(endTime);
    }

    @Test
    void dailyStatisticsSqlShouldUseBoundTemplateIdsAndSendTimeRange() {
        String sql = new MessageStatisticsSqlProvider().selectTimeDay();

        assertThat(sql).contains(
                "TO_CHAR(r.send_time, 'YYYY-MM-DD')",
                "r.send_time &gt;= #{query.startTime}",
                "r.send_time &lt;= #{query.endTime}",
                "<foreach collection='query.templateIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>");
        assertThat(sql).doesNotContain("${item}", "${query.startTime}", "${query.endTime}");
    }

    @Test
    void openApiShouldExposeTemplateIdsAsStringArray() throws Exception {
        Field field = MessageStatisticsQueryDTO.class.getDeclaredField("templateIds");
        ArraySchema schema = field.getAnnotation(ArraySchema.class);

        assertThat(schema).isNotNull();
        assertThat(schema.schema().type()).isEqualTo("string");
    }

    private StatisticsTimeRow timeRow(String period, long total, long success, long failed) {
        StatisticsTimeRow row = new StatisticsTimeRow();
        row.setPeriod(period);
        row.setTotalCount(total);
        row.setSuccessCount(success);
        row.setFailedCount(failed);
        return row;
    }

    private StatisticsCountRow countRow(long total, long success, long failed) {
        StatisticsCountRow row = new StatisticsCountRow();
        row.setTotalCount(total);
        row.setSuccessCount(success);
        row.setFailedCount(failed);
        return row;
    }
}
