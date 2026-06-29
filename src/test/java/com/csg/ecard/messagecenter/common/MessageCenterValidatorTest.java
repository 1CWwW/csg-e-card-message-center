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
        for (String reservedWord : new String[]{
                "true", "false", "null", "undefined", "if", "else",
                "for", "while", "return", "function", "var", "let", "const",
                "new", "this", "class", "switch", "case", "break", "continue",
                "try", "catch", "finally", "throw", "async", "await"
        }) {
            assertThat(MessageCenterValidator.isReservedParamName(reservedWord)).isTrue();
            assertThat(MessageCenterValidator.isValidParamName(reservedWord)).isFalse();
        }
        assertThat(MessageCenterValidator.isReservedParamName("VAR")).isTrue();
        assertThat(MessageCenterValidator.isValidParamName("return")).isFalse();
        assertThat(MessageCenterValidator.isValidParamName("function")).isFalse();
    }
}
