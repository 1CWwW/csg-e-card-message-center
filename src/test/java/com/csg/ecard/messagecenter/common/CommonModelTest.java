package com.csg.ecard.messagecenter.common;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.DeleteFlag;
import com.csg.ecard.messagecenter.common.page.PageRequest;
import com.csg.ecard.messagecenter.common.utils.MessageIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    void shouldGenerateMessageIdByLocalFallbackWhenRedisUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis unavailable"));
        MessageIdGenerator generator = new MessageIdGenerator(redisTemplate);

        String messageId = generator.nextId();

        assertThat(messageId).matches(Pattern.compile("^MSG_\\d{8}_\\d{5,}$"));
    }
}
