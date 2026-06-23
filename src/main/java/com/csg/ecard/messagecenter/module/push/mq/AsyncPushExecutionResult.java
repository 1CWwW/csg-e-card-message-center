package com.csg.ecard.messagecenter.module.push.mq;

import java.util.List;

/**
 * 异步推送单次消费执行结果。
 *
 * @param retryTemplateIds 需要再次投递的模板ID
 */
public record AsyncPushExecutionResult(List<Long> retryTemplateIds) {

    public boolean requiresRetry() {
        return retryTemplateIds != null && !retryTemplateIds.isEmpty();
    }
}
