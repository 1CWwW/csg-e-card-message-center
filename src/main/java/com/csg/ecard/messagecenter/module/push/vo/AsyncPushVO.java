package com.csg.ecard.messagecenter.module.push.vo;

import com.csg.ecard.messagecenter.module.push.enums.AsyncPushStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 异步消息推送受理响应。
 */
@Getter
@Setter
@Schema(description = "异步消息推送受理响应")
public class AsyncPushVO {

    private String msgId;
    private AsyncPushStatus status;
}
