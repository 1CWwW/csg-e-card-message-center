package com.csg.ecard.messagecenter.task;

import com.csg.ecard.messagecenter.module.push.service.ScheduledMessageDispatchService;
import com.csg.ecard.messagecenter.module.push.vo.ScheduledDispatchResultVO;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定时消息到点发送任务入口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageScheduleDispatchJobHandler {

    private final ScheduledMessageDispatchService scheduledMessageDispatchService;

    /**
     * 在 XXL-Job Admin 中配置 JobHandler 为 {@code messageScheduleDispatchHandler}。
     */
    @XxlJob("messageScheduleDispatchHandler")
    public void messageScheduleDispatchHandler() {
        ScheduledDispatchResultVO result = scheduledMessageDispatchService.dispatchDueMessages();
        log.info("Scheduled message dispatch finished, scanned={}, sent={}, failed={}, skipped={}",
                result.getScannedCount(), result.getSentCount(), result.getFailedCount(), result.getSkippedCount());
    }
}
