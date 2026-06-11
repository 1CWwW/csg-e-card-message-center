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
