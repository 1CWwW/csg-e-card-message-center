package com.csg.ecard.messagecenter.module.push.controller;

import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.push.dto.SyncPushDTO;
import com.csg.ecard.messagecenter.module.push.exception.MessagePushException;
import com.csg.ecard.messagecenter.module.push.service.MessagePushService;
import com.csg.ecard.messagecenter.module.push.vo.AsyncPushVO;
import com.csg.ecard.messagecenter.module.push.vo.SyncPushVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息推送接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "消息推送")
@RequestMapping("/api/message-center/push")
public class MessagePushController {

    private final MessagePushService messagePushService;

    @PostMapping("/sync")
    @Operation(summary = "同步推送消息",
            description = "priority为消息业务优先级，可选HIGH、NORMAL、LOW，未传默认NORMAL；"
                    + "它不等于渠道匹配优先级，同步推送不经过RabbitMQ")
    public ApiResult<SyncPushVO> pushSync(@RequestBody @Valid SyncPushDTO request) {
        try {
            return ApiResult.success(messagePushService.pushSync(request));
        } catch (MessagePushException ex) {
            throw ex;
        } catch (BizException | IllegalArgumentException ex) {
            throw MessagePushException.badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Message push internal error", ex);
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            throw MessagePushException.internalError("消息推送内部错误：" + detail);
        }
    }

    @PostMapping("/async")
    @Operation(summary = "异步推送消息",
            description = "priority为消息业务优先级，可选HIGH、NORMAL、LOW，未传默认NORMAL；"
                    + "异步推送会映射为RabbitMQ主队列优先级，但不会抢占已经开始处理的消息；"
                    + "自动重试保持原优先级，且priority不参与渠道匹配")
    public ApiResult<AsyncPushVO> pushAsync(@RequestBody @Valid SyncPushDTO request) {
        try {
            return ApiResult.success(messagePushService.pushAsync(request));
        } catch (MessagePushException ex) {
            throw ex;
        } catch (BizException | IllegalArgumentException ex) {
            throw MessagePushException.badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Async message push internal error", ex);
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            throw MessagePushException.internalError("消息推送内部错误：" + detail);
        }
    }

    /**
     * 按同步推送接口契约返回数字字符串业务码和对应 HTTP 状态。
     */
    @ExceptionHandler(MessagePushException.class)
    public ResponseEntity<ApiResult<Void>> handleMessagePushException(MessagePushException ex) {
        String code = String.valueOf(ex.getHttpStatus().value());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResult.fail(code, ex.getMessage()));
    }
}
