package com.csg.ecard.messagecenter.module.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.CommonStatus;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelCreateDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelPageQueryDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelTypeConfigDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelUpdateDTO;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannel;
import com.csg.ecard.messagecenter.module.channel.entity.MsgChannelUnit;
import com.csg.ecard.messagecenter.module.channel.mapper.ChannelUnitCountResult;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelMapper;
import com.csg.ecard.messagecenter.module.channel.mapper.MsgChannelUnitMapper;
import com.csg.ecard.messagecenter.module.channel.service.impl.MsgChannelServiceImpl;
import com.csg.ecard.messagecenter.module.channel.validator.ChannelTypeConfigValidator;
import com.csg.ecard.messagecenter.module.channel.vo.MsgChannelVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class MsgChannelServiceImplTest {

    private MsgChannelMapper msgChannelMapper;
    private MsgChannelUnitMapper msgChannelUnitMapper;
    private MsgChannelServiceImpl msgChannelService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        msgChannelMapper = mock(MsgChannelMapper.class);
        msgChannelUnitMapper = mock(MsgChannelUnitMapper.class);
        objectMapper = new ObjectMapper();
        msgChannelService = new MsgChannelServiceImpl(
                msgChannelMapper,
                msgChannelUnitMapper,
                new ChannelTypeConfigValidator(),
                objectMapper);
    }

    @Test
    void shouldRejectDuplicateChannelNameWhenCreating() {
        ChannelCreateDTO request = smsCreateRequest();
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_DUPLICATE.getCode()));
    }

    @Test
    void shouldCreateSmsChannelWhenConfigValid() {
        ChannelCreateDTO request = smsCreateRequest();
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(9007199254740993L);
            channel.setCreateTime(LocalDateTime.now());
            channel.setUpdateTime(LocalDateTime.now());
            return 1;
        });
        when(msgChannelUnitMapper.countUniqueEnabledUnits(eq(9007199254740993L), eq(ChannelType.SMS.getCode()))).thenReturn(2L);

        MsgChannelVO result = msgChannelService.create(request);

        assertThat(result.getId()).isEqualTo(9007199254740993L);
        assertThat(result.getChannelType()).isEqualTo(ChannelType.SMS.getCode());
        assertThat(result.getTypeConfig().getSenderNumber()).isEqualTo("13800138000");
        assertThat(result.getUnitCount()).isEqualTo(2L);
        assertThat(result.getUniqueUnitCount()).isEqualTo(2L);
        verify(msgChannelUnitMapper, org.mockito.Mockito.times(2)).insert(any(MsgChannelUnit.class));
    }

    @Test
    void shouldRejectSmsWhenSenderNumberMissing() {
        ChannelCreateDTO request = smsCreateRequest();
        request.getTypeConfig().setSenderNumber(null);

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("请输入正确的手机号码"));
    }

    @Test
    void shouldTrimAndValidateSmsSenderNumber() {
        ChannelCreateDTO request = smsCreateRequest();
        request.getTypeConfig().setSenderNumber(" 13800138000 ");
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        MsgChannelVO result = msgChannelService.create(request);

        assertThat(result.getTypeConfig().getSenderNumber()).isEqualTo("13800138000");
    }

    @Test
    void shouldApplyMobileValidationWhenUpdatingSms() {
        MsgChannel existed = channel(1L, ChannelType.SMS, CommonStatus.ENABLE.getCode());
        when(msgChannelMapper.selectById(1L)).thenReturn(existed);
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        ChannelUpdateDTO request = smsUpdateRequest();
        request.getTypeConfig().setSenderNumber("10690000");

        assertThatThrownBy(() -> msgChannelService.update(1L, request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("请输入正确的手机号码"));
    }

    @Test
    void shouldRejectInvalidEmailConfig() {
        ChannelCreateDTO request = createRequest(ChannelType.EMAIL);
        request.getTypeConfig().setSenderEmail("bad-email");

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectEmailWhenSenderEmailMissing() {
        ChannelCreateDTO request = createRequest(ChannelType.EMAIL);
        request.getTypeConfig().setSenderEmail(" ");

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldIgnoreSenderEmailForNonEmailChannel() {
        ChannelCreateDTO request = smsCreateRequest();
        request.getTypeConfig().setSenderEmail("not-an-email");
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        MsgChannelVO result = msgChannelService.create(request);

        assertThat(result.getTypeConfig().getSenderNumber()).isEqualTo("13800138000");
        assertThat(result.getTypeConfig().getSenderEmail()).isNull();
    }

    @Test
    void shouldRejectElinkWhenAppIdMissing() {
        ChannelCreateDTO request = createRequest(ChannelType.ELINK);
        request.getTypeConfig().setAppId(null);

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOf(BizException.class);
    }

    @Test
    void shouldRejectSixDigitPriorityWhenCreatingElinkChannel() {
        ChannelCreateDTO request = createRequest(ChannelType.ELINK);
        request.setPriority(100_000);

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("eLink应用消息优先级最多输入5位数字"));
    }

    @Test
    void shouldAcceptFiveDigitPriorityWhenCreatingElinkChannel() {
        ChannelCreateDTO request = createRequest(ChannelType.ELINK);
        request.setPriority(99_999);
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        MsgChannelVO result = msgChannelService.create(request);

        assertThat(result.getPriority()).isEqualTo(99_999);
    }

    @Test
    void shouldRejectSixDigitPriorityWhenUpdatingElinkChannel() {
        MsgChannel existed = channel(1L, ChannelType.ELINK, CommonStatus.ENABLE.getCode());
        when(msgChannelMapper.selectById(1L)).thenReturn(existed);
        ChannelUpdateDTO request = updateRequest(ChannelType.ELINK);
        request.setPriority(100_000);

        assertThatThrownBy(() -> msgChannelService.update(1L, request))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getMessage()).isEqualTo("eLink应用消息优先级最多输入5位数字"));
    }

    @Test
    void shouldIgnoreSenderNumberForNonSmsChannel() {
        ChannelCreateDTO request = createRequest(ChannelType.IN_APP);
        request.getTypeConfig().setSenderNumber("10690000");
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        MsgChannelVO result = msgChannelService.create(request);

        assertThat(result.getTypeConfig().getSenderNumber()).isNull();
    }

    @Test
    void shouldRejectNonPositivePriority() {
        ChannelCreateDTO request = smsCreateRequest();
        request.setPriority(0);

        assertThatThrownBy(() -> msgChannelService.create(request))
                .isInstanceOf(BizException.class);
    }

    @Test
    void shouldDefaultCreateStatusToEnable() {
        ChannelCreateDTO request = smsCreateRequest();
        request.setStatus(null);
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        msgChannelService.create(request);

        ArgumentCaptor<MsgChannel> captor = ArgumentCaptor.forClass(MsgChannel.class);
        verify(msgChannelMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatus.ENABLE.getCode());
    }

    @Test
    void shouldCreateAllApplicableChannelWhenUnitIdsEmpty() {
        ChannelCreateDTO request = smsCreateRequest();
        request.setUnitIds(List.of(" ", ""));
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        MsgChannelVO result = msgChannelService.create(request);

        assertThat(result.getUnitIds()).isEmpty();
        assertThat(result.getUnitCount()).isZero();
        verify(msgChannelUnitMapper, never()).insert(any(MsgChannelUnit.class));
    }

    @Test
    void shouldDeduplicateUnitIdsWhenCreating() {
        ChannelCreateDTO request = smsCreateRequest();
        request.setUnitIds(List.of("UNIT_A", " UNIT_A ", "UNIT_B"));
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelMapper.insert(any(MsgChannel.class))).thenAnswer(invocation -> {
            MsgChannel channel = invocation.getArgument(0);
            channel.setId(1L);
            return 1;
        });

        msgChannelService.create(request);

        ArgumentCaptor<MsgChannelUnit> captor = ArgumentCaptor.forClass(MsgChannelUnit.class);
        verify(msgChannelUnitMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(MsgChannelUnit::getUnitId)
                .containsExactly("UNIT_A", "UNIT_B");
    }

    @Test
    void shouldNotModifyChannelTypeWhenUpdating() {
        MsgChannel existed = channel(1L, ChannelType.EMAIL, CommonStatus.ENABLE.getCode());
        MsgChannel updated = channel(1L, ChannelType.EMAIL, CommonStatus.DISABLE.getCode());
        updated.setChannelName("EMAIL_CHANNEL_NEW");
        when(msgChannelMapper.selectById(1L)).thenReturn(existed, updated);
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelUnitMapper.selectUnitIdsByChannelId(1L)).thenReturn(List.of("UNIT_A"));
        when(msgChannelUnitMapper.countUniqueEnabledUnits(eq(1L), eq(ChannelType.EMAIL.getCode()))).thenReturn(1L);

        MsgChannelVO result = msgChannelService.update(1L, emailUpdateRequest());

        ArgumentCaptor<MsgChannel> captor = ArgumentCaptor.forClass(MsgChannel.class);
        verify(msgChannelMapper).updateById(captor.capture());
        assertThat(captor.getValue().getChannelType()).isNull();
        assertThat(result.getChannelType()).isEqualTo(ChannelType.EMAIL.getCode());
    }

    @Test
    void shouldDeleteThenInsertUnitsWhenUpdating() {
        MsgChannel existed = channel(1L, ChannelType.SMS, CommonStatus.ENABLE.getCode());
        when(msgChannelMapper.selectById(1L)).thenReturn(existed, existed);
        when(msgChannelMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(msgChannelUnitMapper.selectUnitIdsByChannelId(1L)).thenReturn(List.of("UNIT_B"));

        msgChannelService.update(1L, smsUpdateRequest());

        InOrder inOrder = inOrder(msgChannelUnitMapper);
        inOrder.verify(msgChannelUnitMapper).deleteByChannelId(1L);
        inOrder.verify(msgChannelUnitMapper).insert(any(MsgChannelUnit.class));
    }

    @Test
    void shouldBuildListQueryAndBatchCountUnits() {
        ChannelPageQueryDTO query = new ChannelPageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setChannelName("鐭俊");
        query.setChannelType(ChannelType.SMS.getCode());
        query.setStatus(CommonStatus.ENABLE.getCode());
        query.setUnitId("UNIT_A");
        Page<MsgChannel> pageResult = new Page<>(1, 20);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(channel(1L, ChannelType.SMS, CommonStatus.ENABLE.getCode())));
        when(msgChannelMapper.selectChannelPage(any(Page.class), eq(query))).thenReturn(pageResult);
        ChannelUnitCountResult countResult = new ChannelUnitCountResult();
        countResult.setChannelId(1L);
        countResult.setUnitCount(3L);
        when(msgChannelUnitMapper.selectUnitCountsByChannelIds(any())).thenReturn(List.of(countResult));

        PageResult<MsgChannelVO> result = msgChannelService.page(query);

        verify(msgChannelMapper).selectChannelPage(any(Page.class), eq(query));
        verify(msgChannelUnitMapper).selectUnitCountsByChannelIds(List.of(1L));
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getUnitCount()).isEqualTo(3L);
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    void shouldAcceptPrioritySortAfterWhitelistValidation() {
        ChannelPageQueryDTO query = pageQuery();
        query.setSortField(" priority ");
        query.setSortOrder(" ASC ");
        when(msgChannelMapper.selectChannelPage(any(Page.class), eq(query)))
                .thenReturn(new Page<>(1, 20));

        msgChannelService.page(query);

        assertThat(query.getSortField()).isEqualTo("priority");
        assertThat(query.getSortOrder()).isEqualTo("ASC");
        verify(msgChannelMapper).selectChannelPage(any(Page.class), eq(query));
    }

    @Test
    void shouldRejectInvalidChannelPageSort() {
        ChannelPageQueryDTO invalidField = pageQuery();
        invalidField.setSortField("createTime");
        invalidField.setSortOrder("ASC");
        ChannelPageQueryDTO invalidOrder = pageQuery();
        invalidOrder.setSortField("priority");
        invalidOrder.setSortOrder("asc");
        ChannelPageQueryDTO incompleteSort = pageQuery();
        incompleteSort.setSortField("priority");

        assertThatThrownBy(() -> msgChannelService.page(invalidField))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> msgChannelService.page(invalidOrder))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> msgChannelService.page(incompleteSort))
                .isInstanceOf(BizException.class);
        verify(msgChannelMapper, never()).selectChannelPage(any(Page.class), any(ChannelPageQueryDTO.class));
    }

    @Test
    void shouldUseFixedStableOrderingInChannelPageSql() throws NoSuchMethodException {
        Select select = MsgChannelMapper.class
                .getMethod("selectChannelPage", Page.class, ChannelPageQueryDTO.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value());

        assertThat(sql)
                .contains("ORDER BY c.priority ASC, c.create_time DESC, c.id DESC")
                .contains("ORDER BY c.priority DESC, c.create_time DESC, c.id DESC")
                .contains("ORDER BY c.create_time DESC, c.id DESC")
                .doesNotContain("${");
    }

    @Test
    void shouldDeleteChannelAndUnits() {
        when(msgChannelMapper.selectById(1L)).thenReturn(channel(1L, ChannelType.SMS, CommonStatus.ENABLE.getCode()));

        msgChannelService.delete(1L);

        verify(msgChannelUnitMapper).deleteByChannelId(1L);
        verify(msgChannelMapper).deleteById(1L);
    }

    private ChannelPageQueryDTO pageQuery() {
        ChannelPageQueryDTO query = new ChannelPageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        return query;
    }

    @Test
    void shouldToggleWithoutRequestBodyAndReturnNewStatus() {
        MsgChannel channel = channel(1L, ChannelType.SMS, CommonStatus.ENABLE.getCode());
        when(msgChannelMapper.selectById(1L)).thenReturn(channel);
        when(msgChannelUnitMapper.selectUnitIdsByChannelId(1L)).thenReturn(List.of("UNIT_A"));

        MsgChannelVO result = msgChannelService.toggle(1L);

        ArgumentCaptor<MsgChannel> captor = ArgumentCaptor.forClass(MsgChannel.class);
        verify(msgChannelMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
        assertThat(result.getStatus()).isEqualTo(CommonStatus.DISABLE.getCode());
    }

    @Test
    void shouldThrowWhenChannelNotFound() {
        when(msgChannelMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> msgChannelService.detail(404L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void shouldSerializeIdAsStringAndCountsAsNumbers() throws Exception {
        MsgChannelVO vo = new MsgChannelVO();
        vo.setId(9007199254740993L);
        vo.setUnitCount(2L);
        vo.setPriority(1);
        vo.setStatus(1);

        JsonNode json = objectMapper.valueToTree(vo);

        assertThat(json.get("id").isTextual()).isTrue();
        assertThat(json.get("id").asText()).isEqualTo("9007199254740993");
        assertThat(json.get("unitCount").isNumber()).isTrue();
        assertThat(json.get("priority").isNumber()).isTrue();
        assertThat(json.get("status").isNumber()).isTrue();
    }

    private ChannelCreateDTO smsCreateRequest() {
        return createRequest(ChannelType.SMS);
    }

    private ChannelCreateDTO createRequest(ChannelType channelType) {
        ChannelCreateDTO request = new ChannelCreateDTO();
        request.setChannelName(channelType.getDesc() + "娓犻亾");
        request.setChannelType(channelType.getCode());
        request.setTypeConfig(typeConfig(channelType));
        request.setPriority(1);
        request.setStatus(CommonStatus.ENABLE.getCode());
        request.setUnitIds(List.of("UNIT_A", "UNIT_B"));
        return request;
    }

    private ChannelUpdateDTO smsUpdateRequest() {
        ChannelUpdateDTO request = new ChannelUpdateDTO();
        request.setChannelName("SMS_CHANNEL_NEW");
        request.setTypeConfig(typeConfig(ChannelType.SMS));
        request.setPriority(2);
        request.setStatus(CommonStatus.ENABLE.getCode());
        request.setUnitIds(List.of("UNIT_B"));
        return request;
    }

    private ChannelUpdateDTO emailUpdateRequest() {
        return updateRequest(ChannelType.EMAIL);
    }

    private ChannelUpdateDTO updateRequest(ChannelType channelType) {
        ChannelUpdateDTO request = new ChannelUpdateDTO();
        request.setChannelName(channelType.getCode() + "_CHANNEL_NEW");
        request.setTypeConfig(typeConfig(channelType));
        request.setPriority(2);
        request.setStatus(CommonStatus.DISABLE.getCode());
        request.setUnitIds(List.of("UNIT_A"));
        return request;
    }

    private ChannelTypeConfigDTO typeConfig(ChannelType channelType) {
        ChannelTypeConfigDTO config = new ChannelTypeConfigDTO();
        if (channelType == ChannelType.SMS) {
            config.setSenderNumber("13800138000");
        } else if (channelType == ChannelType.EMAIL) {
            config.setSenderEmail("sender@example.com");
        } else if (channelType == ChannelType.ELINK) {
            config.setAppId("APP001");
        }
        return config;
    }

    private MsgChannel channel(Long id, ChannelType channelType, Integer status) {
        MsgChannel channel = new MsgChannel();
        channel.setId(id);
        channel.setChannelName(channelType.getDesc() + "娓犻亾");
        channel.setChannelType(channelType.getCode());
        channel.setTypeConfig(typeConfigJson(channelType));
        channel.setPriority(1);
        channel.setStatus(status);
        channel.setCreateTime(LocalDateTime.now().minusMinutes(1));
        channel.setUpdateTime(LocalDateTime.now());
        return channel;
    }

    private String typeConfigJson(ChannelType channelType) {
        return switch (channelType) {
            case SMS -> "{\"senderNumber\":\"13800138000\"}";
            case EMAIL -> "{\"senderEmail\":\"sender@example.com\"}";
            case ELINK -> "{\"appId\":\"APP001\"}";
            case IN_APP -> "{}";
        };
    }
}
