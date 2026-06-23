package com.csg.ecard.messagecenter.module.push.sender;

/**
 * 渠道适配器发送结果。
 *
 * @param success   是否成功
 * @param errorMsg  失败原因
 * @param retryable 是否允许重试
 */
public record ChannelSendResult(boolean success, String errorMsg, boolean retryable) {

    public static ChannelSendResult succeeded() {
        return new ChannelSendResult(true, null, false);
    }

    public static ChannelSendResult failed(String errorMsg) {
        return new ChannelSendResult(false, errorMsg, true);
    }

    public static ChannelSendResult failedNonRetryable(String errorMsg) {
        return new ChannelSendResult(false, errorMsg, false);
    }
}
