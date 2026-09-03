package com.csg.ecard.messagecenter.module.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.utils.RedisUtil;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSenderDispatcher;
import com.csg.ecard.messagecenter.module.record.assembler.MessageRecordAssembler;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordPageQueryDTO;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordMapper;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordQueryCriteria;
import com.csg.ecard.messagecenter.module.record.mapper.MsgRecordResendLogMapper;
import com.csg.ecard.messagecenter.module.record.service.impl.MessageRecordServiceImpl;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordListVO;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 模板近三十天发送记录查询测试。
 */
@ExtendWith(MockitoExtension.class)
class MessageRecordRecentQueryTest {

    private static final String TEMPLATE_ID = "2085629226374467586";
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 8, 2, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

    @Mock
    private MessageRecordMapper messageRecordMapper;
    @Mock
    private MsgRecordResendLogMapper resendLogMapper;
    @Mock
    private MsgSceneParamMapper msgSceneParamMapper;
    @Mock
    private MsgTemplateMapper msgTemplateMapper;
    @Mock
    private MsgChannelMapper msgChannelMapper;
    @Mock
    private ChannelSenderDispatcher channelSenderDispatcher;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private MessageRecordAssembler assembler;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageRecordServiceImpl messageRecordService;

    @Test
    void shouldReturnRealTotalForRecentTemplateRecordsWithPageSizeOne() {
        stubRecords(List.of(
                record(TEMPLATE_ID, START_TIME.plusDays(1)),
                record(TEMPLATE_ID, END_TIME.minusHours(1)),
                record(TEMPLATE_ID, START_TIME.minusSeconds(1))));

        var result = messageRecordService.page(query(TEMPLATE_ID));

        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getPageSize()).isEqualTo(1L);

        ArgumentCaptor<MessageRecordQueryCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(MessageRecordQueryCriteria.class);
        org.mockito.Mockito.verify(messageRecordMapper)
                .selectRecordPage(any(Page.class), criteriaCaptor.capture());
        MessageRecordQueryCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getTemplateId()).isEqualTo(2085629226374467586L);
        assertThat(criteria.getStartTime()).isEqualTo(START_TIME);
        assertThat(criteria.getEndTime()).isEqualTo(END_TIME);
    }

    @Test
    void shouldReturnZeroWhenTemplateHasNoRecentRecord() {
        stubRecords(List.of(record("10", END_TIME.minusDays(1))));

        var result = messageRecordService.page(query(TEMPLATE_ID));

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    void shouldExcludeRecordOlderThanRequestedStartTime() {
        stubRecords(List.of(record(TEMPLATE_ID, START_TIME.minusDays(1))));

        var result = messageRecordService.page(query(TEMPLATE_ID));

        assertThat(result.getTotal()).isZero();
    }

    @Test
    void recordListSqlShouldUseBoundTemplateIdAndSendTimeRange() throws Exception {
        Select select = MessageRecordMapper.class
                .getMethod("selectRecordPage", Page.class, MessageRecordQueryCriteria.class)
                .getAnnotation(Select.class);
        String sql = String.join("\n", select.value());

        assertThat(sql).contains(
                "r.template_id = #{query.templateId}",
                "r.send_time &gt;= #{query.startTime}",
                "r.send_time &lt;= #{query.endTime}");
        assertThat(sql).doesNotContain("${query.templateId}", "${query.startTime}", "${query.endTime}");
    }

    @Test
    void recipientKeywordShouldFuzzyMatchNameIdAndCombinedValue() throws Exception {
        String listSql = selectSql("selectRecordPage");
        String exportSql = selectSql("selectExportPage");

        assertRecipientKeywordSql(listSql);
        assertRecipientKeywordSql(exportSql);
    }

    @Test
    void overviewShouldUseLatestSendTimeAndFallbackToCreateTimeForUnsentRecords()
            throws Exception {
        Select select = MessageRecordMapper.class
                .getMethod("selectOverview", LocalDateTime.class,
                        LocalDateTime.class, LocalDateTime.class)
                .getAnnotation(Select.class);
        String sql = String.join("\n", select.value());

        assertThat(sql).contains(
                "COALESCE(r.send_time, r.create_time) >= #{todayStart}",
                "COALESCE(r.send_time, r.create_time) < #{tomorrowStart}",
                "AND r.send_status = 'SUCCESS'",
                "AND r.send_status = 'FAILED'",
                "AND r.send_status IN ('PENDING', 'ACCEPTED')");
        assertThat(sql).doesNotContain("CASE WHEN r.create_time >= #{todayStart}");
    }

    private void stubRecords(List<RecordFixture> fixtures) {
        when(messageRecordMapper.selectRecordPage(any(Page.class), any(MessageRecordQueryCriteria.class)))
                .thenAnswer(invocation -> {
                    Page<MessageRecordListVO> page = invocation.getArgument(0);
                    MessageRecordQueryCriteria criteria = invocation.getArgument(1);
                    long total = fixtures.stream()
                            .filter(item -> Long.valueOf(item.templateId()).equals(criteria.getTemplateId()))
                            .filter(item -> criteria.getStartTime() == null
                                    || !item.sendTime().isBefore(criteria.getStartTime()))
                            .filter(item -> criteria.getEndTime() == null
                                    || !item.sendTime().isAfter(criteria.getEndTime()))
                            .count();
                    page.setTotal(total);
                    page.setRecords(total == 0 ? List.of() : List.of(new MessageRecordListVO()));
                    return page;
                });
    }

    private MessageRecordPageQueryDTO query(String templateId) {
        MessageRecordPageQueryDTO query = new MessageRecordPageQueryDTO();
        query.setTemplateId(templateId);
        query.setStartTime(START_TIME);
        query.setEndTime(END_TIME);
        query.setPageNum(1);
        query.setPageSize(1);
        return query;
    }

    private RecordFixture record(String templateId, LocalDateTime sendTime) {
        return new RecordFixture(templateId, sendTime);
    }

    private String selectSql(String methodName) throws Exception {
        Select select = MessageRecordMapper.class
                .getMethod(methodName, Page.class, MessageRecordQueryCriteria.class)
                .getAnnotation(Select.class);
        return String.join("\n", select.value());
    }

    private void assertRecipientKeywordSql(String sql) {
        assertThat(sql).contains(
                "r.user_name LIKE '%' || #{query.userId} || '%'",
                "r.user_id LIKE '%' || #{query.userId} || '%'",
                "COALESCE(r.user_name, '') || COALESCE(r.user_id, '')");
        assertThat(sql).doesNotContain("r.user_id = #{query.userId}", "${query.userId}");
    }

    private record RecordFixture(String templateId, LocalDateTime sendTime) {
    }
}
