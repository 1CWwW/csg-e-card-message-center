package com.csg.ecard.messagecenter.demo;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.csg.ecard.messagecenter.common.utils.IdempotentService;
import com.csg.ecard.messagecenter.common.utils.MessageIdGenerator;
import com.csg.ecard.messagecenter.common.utils.RabbitMessageSender;
import com.csg.ecard.messagecenter.common.utils.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础框架 Demo 验证接口。
 * <p>
 * 仅用于验证服务启动、统一返回、异常处理、参数校验、Redis、RabbitMQ、数据库连接、
 * 消息 ID 生成和幂等占位能力，不承载消息中心业务逻辑。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Demo验证接口", description = "仅用于验证基础框架能力")
@RequestMapping("/demo")
public class DemoController {

    private final RedisUtil redisUtil;
    private final RabbitMessageSender rabbitMessageSender;
    private final MessageIdGenerator messageIdGenerator;
    private final IdempotentService idempotentService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 验证服务是否正常启动。
     *
     * @return 服务基础状态
     */
    @GetMapping("/ping")
    @Operation(summary = "服务启动验证")
    public ApiResult<Map<String, Object>> ping() {
        return ApiResult.success(Map.of(
                "service", "csg-e-card-message-center",
                "status", "UP"
        ));
    }

    /**
     * 验证业务异常能否被全局异常处理器转换为统一返回。
     *
     * @return 不会正常返回
     */
    @GetMapping("/exception")
    @Operation(summary = "全局异常验证")
    public ApiResult<Void> exception() {
        throw new BizException(ErrorCode.BUSINESS_ERROR, "Demo业务异常");
    }

    /**
     * 验证查询参数校验。
     *
     * @param name 非空名称
     * @return 问候文本
     */
    @GetMapping("/validate")
    @Operation(summary = "参数校验验证")
    public ApiResult<String> validate(@RequestParam @NotBlank(message = "name不能为空") String name) {
        return ApiResult.success("hello " + name);
    }

    /**
     * 验证请求体参数校验。
     *
     * @param request Demo 请求体
     * @return 原样返回请求体
     */
    @PostMapping("/validate-body")
    @Operation(summary = "请求体参数校验验证")
    public ApiResult<DemoValidateRequest> validateBody(@RequestBody @Valid DemoValidateRequest request) {
        return ApiResult.success(request);
    }

    /**
     * 验证 Redis 基础读写能力。
     *
     * @param key   Redis key
     * @param value 写入值
     * @return 读取到的缓存内容
     */
    @GetMapping("/redis")
    @Operation(summary = "Redis读写验证")
    public ApiResult<Map<String, Object>> redis(@RequestParam(defaultValue = "demo:message-center") String key,
                                                @RequestParam(defaultValue = "ok") String value) {
        redisUtil.set(key, value, Duration.ofMinutes(5));
        Object cached = redisUtil.get(key);
        return ApiResult.success(Map.of("key", key, "value", cached));
    }

    /**
     * 验证 RabbitMQ 基础发送能力。
     *
     * @param exchange   交换机名称
     * @param routingKey 路由键
     * @param body       消息体；为空时使用空对象
     * @return 消息 correlationId
     */
    @PostMapping("/rabbitmq")
    @Operation(summary = "RabbitMQ发送验证")
    public ApiResult<Map<String, Object>> rabbitmq(@RequestParam String exchange,
                                                   @RequestParam String routingKey,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> message = body == null ? new HashMap<>() : new HashMap<>(body);
        message.putIfAbsent("source", "demo");
        String correlationId = rabbitMessageSender.send(exchange, routingKey, message);
        return ApiResult.success(Map.of("correlationId", correlationId));
    }

    /**
     * 验证达梦数据库连接可用性。
     *
     * @return 数据库产品信息和连接有效性
     * @throws Exception 获取连接元数据失败时抛出，由全局异常处理器统一处理
     */
    @GetMapping("/db")
    @Operation(summary = "达梦数据库连接验证")
    public ApiResult<Map<String, Object>> db() throws Exception {
        Map<String, Object> result = jdbcTemplate.execute((ConnectionCallback<Map<String, Object>>) connection ->
                Map.of(
                    "databaseProductName", connection.getMetaData().getDatabaseProductName(),
                    "databaseProductVersion", connection.getMetaData().getDatabaseProductVersion(),
                    "valid", connection.isValid(3)
                ));
        return ApiResult.success(result);
    }

    /**
     * 验证消息 ID 生成能力。
     *
     * @return 消息 ID
     */
    @GetMapping("/message-id")
    @Operation(summary = "消息ID生成验证")
    public ApiResult<Map<String, Object>> messageId() {
        return ApiResult.success(Map.of("messageId", messageIdGenerator.nextId()));
    }

    /**
     * 验证幂等占位能力。
     *
     * @param bizKey 业务幂等 key
     * @return 是否成功获取占位
     */
    @GetMapping("/idempotent")
    @Operation(summary = "幂等占位能力验证")
    public ApiResult<Map<String, Object>> idempotent(@RequestParam @NotBlank String bizKey) {
        boolean acquired = idempotentService.tryAcquire(bizKey);
        return ApiResult.success(Map.of("bizKey", bizKey, "acquired", acquired));
    }
}
