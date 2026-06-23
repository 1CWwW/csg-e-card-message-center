package com.csg.ecard.messagecenter.module.push.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 同步消息推送接口专属异常。
 */
@Getter
public class MessagePushException extends RuntimeException {

    private final HttpStatus httpStatus;

    public MessagePushException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public static MessagePushException badRequest(String message) {
        return new MessagePushException(HttpStatus.BAD_REQUEST, message);
    }

    public static MessagePushException internalError(String message) {
        return new MessagePushException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
