package com.csg.ecard.messagecenter.task;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-Job 示例任务处理器。
 * <p>
 * 用于验证 Executor 与调度中心集成，不包含真实业务调度逻辑。
 */
@Slf4j
@Component
public class XxlJobDemoHandler {

    /**
     * Demo 任务入口。
     * <p>
     * 在 XXL-Job Admin 中配置 JobHandler 为 {@code messageCenterDemoHandler} 后可触发执行。
     */
    @XxlJob("messageCenterDemoHandler")
    public void messageCenterDemoHandler() {
        log.info("XXL-Job demo handler executed");
    }
}
