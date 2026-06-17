package com.csg.ecard.messagecenter.common;

import com.csg.ecard.messagecenter.common.utils.MessageCenterValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCenterValidatorTest {

    @Test
    void shouldValidateSceneCode() {
        assertThat(MessageCenterValidator.isValidSceneCode("CANTEEN_DEDUCTION")).isTrue();
        assertThat(MessageCenterValidator.isValidSceneCode("canteen_DEDUCTION")).isFalse();
        assertThat(MessageCenterValidator.isValidSceneCode("1CANTEEN")).isFalse();
        assertThat(MessageCenterValidator.isValidSceneCode("CANTEEN-DEDUCTION")).isFalse();
        assertThat(MessageCenterValidator.isValidSceneCode(null)).isFalse();
    }

    @Test
    void shouldValidateParamName() {
        assertThat(MessageCenterValidator.isValidParamName("merchantName")).isTrue();
        assertThat(MessageCenterValidator.isValidParamName("merchant1")).isTrue();
        assertThat(MessageCenterValidator.isValidParamName("1merchant")).isFalse();
        assertThat(MessageCenterValidator.isValidParamName("merchant_name")).isFalse();
        assertThat(MessageCenterValidator.isValidParamName(null)).isFalse();
    }

    @Test
    void shouldRejectReservedParamName() {
        assertThat(MessageCenterValidator.isReservedParamName("true")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("false")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("null")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("undefined")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("if")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("else")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("for")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("while")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("return")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("function")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("VAR")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("let")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("const")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("break")).isTrue();
        assertThat(MessageCenterValidator.isReservedParamName("continue")).isTrue();
        assertThat(MessageCenterValidator.isValidParamName("return")).isFalse();
        assertThat(MessageCenterValidator.isValidParamName("function")).isFalse();
    }
}
