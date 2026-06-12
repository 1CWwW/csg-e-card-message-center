# csg-e-card-message-center

电子卡消息中心后端基础框架。当前版本只包含 Spring Boot 单体后端工程骨架、基础配置、通用返回、全局异常、基础工具和 Demo 验证接口，不包含登录认证、权限、消息中心业务模块、场景管理、渠道管理、模板管理、消息推送、消息记录或统计报表代码。

## 技术栈

- Java 17
- Spring Boot 3.x
- Maven 3.8+
- MyBatis-Plus 3.5+
- RabbitMQ 3.12+
- Redis 6.0+
- XXL-Job 2.4+
- 达梦数据库
- Knife4j / OpenAPI 3
- Jackson
- SLF4J + Logback
- Hutool

## 目录结构

```text
com.csg.ecard.messagecenter
├── common/              ← 公共组件（无业务语义）
│   ├── constant/        ← 全局常量
│   ├── entity/          ← 基础实体基类
│   ├── enums/           ← 错误码枚举
│   ├── exception/       ← 业务异常
│   ├── page/            ← 分页请求/响应
│   ├── result/          ← 统一返回 ApiResult
│   └── utils/           ← 工具类 (Redis、MQ发送、幂等、ID生成)
├── config/              ← 第三方集成配置
│   ├── jackson/         ← Jackson 全局配置
│   ├── mybatis/         ← MyBatis-Plus 分页插件 + 自动填充
│   ├── rabbitmq/        ← RabbitMQ 消息转换 + Confirm/Callback
│   ├── redis/           ← Redis 序列化配置
│   ├── swagger/         ← Knife4j/OpenAPI 文档配置
│   ├── web/             ← Web 过滤器注册
│   └── xxljob/          ← XXL-Job 配置
├── framework/           ← 框架基础设施
│   ├── context/         ← 当前用户上下文
│   ├── filter/          ← 请求日志过滤器（TraceId）
│   ├── handler/         ← 全局异常处理器
│   └── log/             ← 日志包声明
├── infrastructure/      ← 基础设施层（预留）
│   ├── channel/         ← 渠道管理（预留）
│   └── employee/        ← 员工管理（预留）
├── mq/                  ← 消息消费者（预留）
├── task/                ← 定时任务（XXL-Job）
├── demo/                ← Demo 验证接口
└── CsgECardMessageCenterApplication.java  ← 入口
```

## 配置文件

- `src/main/resources/application.yml`：公共配置
- `src/main/resources/application-dev.yml`：开发环境配置
- `src/main/resources/application-test.yml`：测试环境配置
- `src/main/resources/application-prod.yml`：生产环境配置
- `src/main/resources/logback-spring.xml`：日志配置

数据库、Redis、RabbitMQ、XXL-Job 的密码均使用环境变量占位符，未写死真实密码。

常用环境变量：

```bash
DM_DB_URL=jdbc:dm://localhost:5236/CSG_ECARD_MESSAGE_CENTER
DM_DB_USERNAME=SYSDBA
DM_DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_password
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=your_password
XXL_JOB_ENABLED=false
XXL_JOB_ADMIN_ADDRESSES=http://localhost:8081/xxl-job-admin
XXL_JOB_ACCESS_TOKEN=your_token
```

## 本地启动

确保本机 Maven 使用 JDK 17：

```bash
mvn -v
```

启动开发环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

默认访问地址：

```text
http://localhost:8080/message-center
```

## Knife4j / Swagger

- Knife4j：`http://localhost:8080/message-center/doc.html`
- OpenAPI JSON：`http://localhost:8080/message-center/v3/api-docs`
- Swagger UI：`http://localhost:8080/message-center/swagger-ui.html`

## Demo 接口

所有 Demo 接口均返回统一结构 `ApiResult<T>`，异常统一由 `GlobalExceptionHandler` 处理。

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/message-center/demo/ping` | 服务启动验证 |
| GET | `/message-center/demo/exception` | 全局业务异常验证 |
| GET | `/message-center/demo/validate?name=test` | 参数校验验证 |
| POST | `/message-center/demo/validate-body` | 请求体参数校验验证 |
| GET | `/message-center/demo/redis?key=demo&value=ok` | Redis 读写验证 |
| POST | `/message-center/demo/rabbitmq?exchange=xxx&routingKey=xxx` | RabbitMQ 发送验证 |
| GET | `/message-center/demo/db` | 达梦数据库连接验证 |
| GET | `/message-center/demo/message-id` | 消息 ID 生成验证，格式 `MSG_yyyyMMdd_000001` |
| GET | `/message-center/demo/idempotent?bizKey=test` | 幂等占位能力验证 |

## 验证命令

```bash
mvn clean package
mvn test
```

如果命令提示 `release version 17 not supported` 或 Maven 输出的 Java version 不是 17，请先切换 `JAVA_HOME` 到 JDK 17 后重新执行。

## 公共模型与通用规则模块

当前公共模块只提供后续业务复用的基础能力，不包含登录认证、权限控制、消息中心业务 CRUD 或操作日志落库。

### 枚举说明

- 通用枚举集中放置在 `common.enums` 包下，包含渠道类型、通用启停状态、参数类型、消息发送状态、消息优先级、模板内容状态、逻辑删除标记和操作结果。
- 枚举统一提供 `code`、`desc` 和 `fromCode()`，业务代码应复用枚举值，避免散落硬编码。
- 错误码统一维护在 `ErrorCode`，与 `ApiResult` 和 `BizException` 保持兼容。

### 编码规则

- 场景编码不能为空，必须以大写字母开头，只允许大写字母、数字、下划线，最大长度 64，例如 `CANTEEN_DEDUCTION`。
- 参数名不能为空，必须以字母开头，只允许字母和数字，最大长度 64，例如 `merchantName`。
- 参数名保留字包括 `true`、`false`、`null`、`undefined`、`if`、`else`、`for`、`while`、`return`、`break`、`continue`。
- 模板名称和渠道名称不能为空，最大长度 50；描述允许为空，非空时最大长度 200。

### 消息 ID 规则

- 消息 ID 格式为 `MSG_yyyyMMdd_序列号`，例如 `MSG_20260527_00001`。
- 序列号至少 5 位，不足补 0。
- 默认优先使用 Redis 按日期自增；Redis 不可用时降级为本地内存序列，仅保证当前 JVM 内并发安全，生产环境建议保障 Redis 可用。

### 分页规则

- 分页请求统一使用 `PageRequest`，包含 `pageNo`、`pageSize`、`keyword`、`orderBy`、`asc`。
- 默认 `pageNo = 1`，默认 `pageSize = 10`，最大 `pageSize = 100`。
- `pageNo` 小于 1 时自动修正为 1；`pageSize` 为空、小于 1 或超过最大值时自动修正为 10。
- 分页结果统一使用 `PageResult<T>`，支持从 MyBatis-Plus `IPage<T>` 转换。

### 当前用户上下文

- `CurrentUserContext` 是当前阶段的占位能力，不实现登录、Token 或权限校验。
- 上下文支持用户 ID、用户名称、所属组织 ID、所属组织名称、请求 IP 和请求 URI。
- 请求结束时由过滤器清理 ThreadLocal，避免线程复用造成上下文残留。

### 幂等能力

- `IdempotentService` 支持通过业务 ID 或请求 ID 做重复请求判断，并支持设置过期时间。
- 默认优先使用 Redis `SET NX EX`；Redis 不可用时降级为本地内存占位，避免公共能力导致项目启动失败。
