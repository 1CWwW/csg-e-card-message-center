package com.csg.ecard.messagecenter.common;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.DeleteFlag;
import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.page.PageRequest;
import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.csg.ecard.messagecenter.common.utils.MessageIdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.csg.ecard.messagecenter.framework.handler.GlobalExceptionHandler;
import com.csg.ecard.messagecenter.module.scene.vo.MsgSceneVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonModelTest {

    @Test
    void shouldConvertEnumFromCode() {
        assertThat(ChannelType.fromCode("SMS")).isEqualTo(ChannelType.SMS);
        assertThat(DeleteFlag.fromCode(0)).isEqualTo(DeleteFlag.NORMAL);
        assertThatThrownBy(() -> ChannelType.fromCode("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNormalizePageRequest() {
        PageRequest request = new PageRequest();
        request.setPageNo(0L);
        request.setPageSize(200L);
        request.setAsc(null);

        request.normalize();

        assertThat(request.getPageNo()).isEqualTo(1L);
        assertThat(request.getPageSize()).isEqualTo(10L);
        assertThat(request.getAsc()).isTrue();
        assertThat(request.offset()).isZero();
    }

    @Test
    void shouldSerializePageResultWithListField() throws Exception {
        PageResult<String> pageResult = PageResult.of(List.of("scene"), 1, 1, 10);

        String json = new ObjectMapper().writeValueAsString(pageResult);

        assertThat(json).contains("\"list\"");
        assertThat(json).contains("\"total\"");
        assertThat(json).doesNotContain("\"records\"");
        assertThat(json).doesNotContain("\"pageNo\"");
        assertThat(json).doesNotContain("\"pageSize\"");
        assertThat(json).doesNotContain("\"pages\"");
    }

    @Test
    void shouldSerializeSceneListWithStringIdAndNumericCounts() throws Exception {
        MsgSceneVO scene = new MsgSceneVO();
        scene.setId(9_007_199_254_740_993L);
        scene.setSceneCode("CANTEEN_CONSUME_SUCCESS");
        scene.setStatus(0);
        scene.setParamCount(0L);
        scene.setTemplateCount(0L);
        PageResult<MsgSceneVO> pageResult = PageResult.of(List.of(scene), 1L, 1, 20);

        JsonNode root = new ObjectMapper().valueToTree(ApiResult.success(pageResult));
        JsonNode first = root.path("data").path("list").get(0);

        assertThat(first.path("id").isTextual()).isTrue();
        assertThat(first.path("id").asText()).isEqualTo("9007199254740993");
        assertThat(first.path("paramCount").isNumber()).isTrue();
        assertThat(first.path("templateCount").isNumber()).isTrue();
        assertThat(first.path("status").isNumber()).isTrue();
        assertThat(root.path("data").path("total").isNumber()).isTrue();
    }

    @Test
    void shouldSerializeSuccessResultWithoutTraceIdAndTimestamp() {
        JsonNode root = new ObjectMapper().valueToTree(ApiResult.success("ok"));

        assertThat(root.has("traceId")).isFalse();
        assertThat(root.has("timestamp")).isFalse();
        assertThat(root.path("code").asText()).isEqualTo(ErrorCode.SUCCESS.getCode());
    }

    @Test
    void shouldSerializeBizExceptionResultWithoutTraceId() {
        ApiResult<Void> result = new GlobalExceptionHandler()
                .handleBizException(new BizException(ErrorCode.DATA_NOT_FOUND));

        JsonNode root = new ObjectMapper().valueToTree(result);

        assertThat(root.has("traceId")).isFalse();
        assertThat(root.has("timestamp")).isFalse();
    }

    @Test
    void shouldSerializeBindExceptionResultWithoutTimestamp() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "pageNum", "pageNum不能为空"));
        BindException bindException = new BindException(bindingResult);

        ApiResult<Void> result = new GlobalExceptionHandler().handleBindException(bindException);
        JsonNode root = new ObjectMapper().valueToTree(result);

        assertThat(root.has("traceId")).isFalse();
        assertThat(root.has("timestamp")).isFalse();
    }

    @Test
    void shouldGenerateMessageIdByLocalFallbackWhenRedisUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis unavailable"));
        MessageIdGenerator generator = new MessageIdGenerator(redisTemplate);

        String messageId = generator.nextId();

        assertThat(messageId).matches(Pattern.compile("^MSG_\\d{8}_\\d{5,}$"));
    }
}
