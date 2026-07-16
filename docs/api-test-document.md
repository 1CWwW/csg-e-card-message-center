# 消息中心接口测试文档

## 1. 测试说明

- 基础地址：`http://localhost:8080`
- 请求头：`Content-Type: application/json`
- 统一返回结构：

```json
{
  "code": "00000",
  "message": "成功",
  "data": {}
}
```

- 分页返回结构：

```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "list": [],
    "total": 0
  }
}
```

- 常用状态：`status=1` 表示启用，`status=0` 表示停用。
- 常用渠道类型：`SMS` 短信、`EMAIL` 邮件、`ELINK` eLink、`IN_APP` 站内信。
- 常用消息优先级：`HIGH`、`NORMAL`、`LOW`。
- 常用场景参数类型：`STRING`、`NUMBER`、`TIME`、`STRING_ARRAY`、`NUMBER_ARRAY`、`OBJECT_ARRAY`。
- Long 类型 ID 在 JSON 响应中按项目配置可能返回字符串；测试时建议把路径 ID 替换为实际返回 ID。

## 2. 推荐测试数据

后续接口示例默认使用以下测试数据：

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "sceneName": "食堂消费成功测试",
  "module": "CANTEEN_CONSUME",
  "unitIds": ["CSG-GD-001", "CSG-GZ-001"],
  "userId": "emp-test-001",
  "userName": "测试员工",
  "userOrgId": "CSG-GZ-001",
  "userOrgName": "广州测试单位",
  "userPhone": "13800000001",
  "userEmail": "tester001@example.com"
}
```

建议测试顺序：

1. 新增场景。
2. 为场景新增参数。
3. 新增渠道。
4. 新增模板。
5. 保存模板内容。
6. 调用模板预览。
7. 调用同步或异步推送。
8. 查询消息记录和统计报表。

## 3. 场景管理

### 3.1 场景分页查询

- 方法：`GET`
- 地址：`/api/msg/scene/list`
- Query：

| 参数 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- |
| pageNum | 是 | `1` | 页码，从 1 开始 |
| pageSize | 是 | `20` | 每页条数，最大 100 |
| sceneCode | 否 | `CANTEEN_CONSUME_SUCCESS_TEST` | 场景编码 |
| sceneName | 否 | `食堂消费` | 场景名称 |
| module | 否 | `CANTEEN_CONSUME` | 所属模块 |
| status | 否 | `1` | 启停状态 |
| sortField | 否 | `createTime` | 当前仅支持 createTime |
| sortOrder | 否 | `desc` | asc 或 desc |

示例：

```http
GET /api/msg/scene/list?pageNum=1&pageSize=20&sceneCode=CANTEEN_CONSUME_SUCCESS_TEST
```

### 3.2 新增场景

- 方法：`POST`
- 地址：`/api/msg/scene`

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "sceneName": "食堂消费成功测试",
  "module": "CANTEEN_CONSUME",
  "description": "接口测试场景",
  "status": 1
}
```

记录响应中的 `data.id`，后续用作 `{sceneId}`。

### 3.3 场景详情

- 方法：`GET`
- 地址：`/api/msg/scene/{sceneId}`

### 3.4 场景编码可用性检查

- 方法：`GET`
- 地址：`/api/msg/scene/check-code`

```http
GET /api/msg/scene/check-code?sceneCode=CANTEEN_CONSUME_SUCCESS_TEST
GET /api/msg/scene/check-code?sceneCode=CANTEEN_CONSUME_SUCCESS_TEST&excludeId={sceneId}
```

### 3.5 编辑场景

- 方法：`PUT`
- 地址：`/api/msg/scene/{sceneId}`

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "sceneName": "食堂消费成功测试-编辑",
  "module": "CANTEEN_CONSUME",
  "description": "接口测试场景编辑",
  "status": 1
}
```

### 3.6 场景停用检查

- 方法：`GET`
- 地址：`/api/msg/scene/{sceneId}/disable-check`

### 3.7 场景启停切换

- 方法：`PUT`
- 地址：`/api/msg/scene/{sceneId}/toggle`

### 3.8 删除场景

- 方法：`DELETE`
- 地址：`/api/msg/scene/{sceneId}`

## 4. 场景参数管理

### 4.1 参数列表查询

- 方法：`GET`
- 地址：`/api/msg/scene/{sceneId}/params`

### 4.2 新增参数

- 方法：`POST`
- 地址：`/api/msg/scene/{sceneId}/params`

```json
{
  "paramName": "merchantName",
  "paramLabel": "商户名称",
  "paramType": "STRING",
  "sortOrder": 1,
  "isRequired": 1
}
```

```json
{
  "paramName": "amount",
  "paramLabel": "消费金额",
  "paramType": "NUMBER",
  "sortOrder": 2,
  "isRequired": 1
}
```

记录响应中的 `data.id`，后续用作 `{paramId}`。

### 4.3 编辑参数

- 方法：`PUT`
- 地址：`/api/msg/scene/{sceneId}/params/{paramId}`

```json
{
  "paramName": "merchantName",
  "paramLabel": "商户名称",
  "paramType": "STRING",
  "sortOrder": 1,
  "isRequired": 1
}
```

### 4.4 参数排序

- 方法：`PUT`
- 地址：`/api/msg/scene/{sceneId}/params/sort`

```json
{
  "items": [
    {
      "paramId": "{paramId1}",
      "sortOrder": 1
    },
    {
      "paramId": "{paramId2}",
      "sortOrder": 2
    }
  ]
}
```

### 4.5 参数引用查询

- 方法：`GET`
- 地址：`/api/msg/scene/{sceneId}/params/{paramId}/usage`

### 4.6 删除参数

- 方法：`DELETE`
- 地址：`/api/msg/scene/{sceneId}/params/{paramId}`

## 5. 渠道管理

### 5.1 渠道分页查询

- 方法：`GET`
- 地址：`/api/msg/channel/list`

```http
GET /api/msg/channel/list?pageNum=1&pageSize=20&channelType=IN_APP&status=1&unitId=CSG-GZ-001
```

### 5.2 新增站内信渠道

- 方法：`POST`
- 地址：`/api/msg/channel`

```json
{
  "channelName": "站内信测试渠道",
  "channelType": "IN_APP",
  "typeConfig": {},
  "priority": 1,
  "status": 1,
  "unitIds": ["CSG-GD-001", "CSG-GZ-001"]
}
```

记录响应中的 `data.id`，后续用作 `{channelId}`。

### 5.3 新增短信渠道

```json
{
  "channelName": "短信测试渠道",
  "channelType": "SMS",
  "typeConfig": {
    "senderNumber": "95598"
  },
  "priority": 2,
  "status": 1,
  "unitIds": ["CSG-GZ-001"]
}
```

### 5.4 新增邮件渠道

```json
{
  "channelName": "邮件测试渠道",
  "channelType": "EMAIL",
  "typeConfig": {
    "senderEmail": "notice@example.com"
  },
  "priority": 3,
  "status": 1,
  "unitIds": ["CSG-GZ-001"]
}
```

### 5.5 新增 eLink 渠道

```json
{
  "channelName": "eLink测试渠道",
  "channelType": "ELINK",
  "typeConfig": {
    "appId": "elink-test-app"
  },
  "priority": 4,
  "status": 1,
  "unitIds": ["CSG-GZ-001"]
}
```

### 5.6 渠道详情

- 方法：`GET`
- 地址：`/api/msg/channel/{channelId}`

### 5.7 编辑渠道

- 方法：`PUT`
- 地址：`/api/msg/channel/{channelId}`

```json
{
  "channelName": "站内信测试渠道-编辑",
  "typeConfig": {},
  "priority": 1,
  "status": 1,
  "unitIds": ["CSG-GZ-001"]
}
```

### 5.8 渠道启停切换

- 方法：`PUT`
- 地址：`/api/msg/channel/{channelId}/toggle`

### 5.9 删除渠道

- 方法：`DELETE`
- 地址：`/api/msg/channel/{channelId}`

## 6. 模板管理

### 6.1 模板分页查询

- 方法：`GET`
- 地址：`/api/msg/template/list`

```http
GET /api/msg/template/list?pageNum=1&pageSize=20&sceneId={sceneId}&channelType=IN_APP&status=1&contentStatus=0
```

`contentStatus`：`0` 全部、`1` 已编辑、`2` 未编辑。

### 6.2 参考模板分页查询

- 方法：`GET`
- 地址：`/api/msg/template/reference-list`

```http
GET /api/msg/template/reference-list?pageNum=1&pageSize=20&channelType=IN_APP&sceneId={sceneId}&contentStatus=1
```

### 6.3 新增模板

- 方法：`POST`
- 地址：`/api/msg/template`

```json
{
  "templateName": "食堂消费成功站内信模板",
  "sceneId": "{sceneId}",
  "channelType": "IN_APP",
  "unitIds": ["CSG-GZ-001"]
}
```

记录响应中的 `data.id`，后续用作 `{templateId}`。

### 6.4 模板详情

- 方法：`GET`
- 地址：`/api/msg/template/{templateId}`

### 6.5 编辑模板基础信息

- 方法：`PUT`
- 地址：`/api/msg/template/{templateId}`

```json
{
  "templateName": "食堂消费成功站内信模板-编辑",
  "channelType": "IN_APP",
  "status": 1,
  "unitIds": ["CSG-GZ-001"]
}
```

### 6.6 保存模板 Blockly 内容

- 方法：`PUT`
- 地址：`/api/msg/template/{templateId}/content`

```json
{
  "schemaVersion": 1,
  "workspace": {
    "blocks": {
      "languageVersion": 0,
      "blocks": [
        {
          "type": "text",
          "id": "text-1",
          "fields": {
            "TEXT": "您在食堂消费成功，请查看消费明细。"
          },
          "x": 20,
          "y": 20
        }
      ]
    }
  }
}
```

说明：模板内容结构由后端 Blockly 校验器校验，实际测试时建议优先使用前端模板编辑器保存出来的 `workspace`。

### 6.7 预览模板正文

- 方法：`POST`
- 地址：`/api/msg/template/preview`

```json
{
  "templateId": "{templateId}",
  "schemaVersion": 1,
  "values": {
    "merchantName": "测试食堂",
    "amount": 12.5
  }
}
```

### 6.8 获取模板场景参数工具箱

- 方法：`GET`
- 地址：`/api/msg/template/{templateId}/toolbox`

### 6.9 查询参考模板

- 方法：`GET`
- 地址：`/api/msg/template/{templateId}/references`

### 6.10 加载参考模板内容

- 方法：`GET`
- 地址：`/api/msg/template/{templateId}/references/{referenceId}`

### 6.11 复制模板

- 方法：`POST`
- 地址：`/api/msg/template/{templateId}/copy`

```json
{
  "templateName": "食堂消费成功站内信模板-复制",
  "sceneId": "{sceneId}",
  "copyContent": true,
  "unitIds": ["CSG-GZ-001"]
}
```

### 6.12 模板启停切换

- 方法：`PUT`
- 地址：`/api/msg/template/{templateId}/toggle`

### 6.13 删除模板

- 方法：`DELETE`
- 地址：`/api/msg/template/{templateId}`

## 7. 消息推送

### 7.1 同步单条推送

- 方法：`POST`
- 地址：`/api/message-center/push/sync`

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "registerCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "receiveCorpId": "CSG-GZ-001",
  "sceneParams": {
    "merchantName": "测试食堂",
    "amount": 12.5
  },
  "userId": "emp-test-001",
  "userName": "测试员工",
  "userOrgId": "CSG-GZ-001",
  "userOrgName": "广州测试单位",
  "registerXtbs": "CANTEEN_CONSUME",
  "type": "NOTICE",
  "title": "食堂消费成功通知",
  "url": "https://example.com/message/detail/test",
  "content": "测试员工在测试食堂消费 12.5 元",
  "senderUserId": "system",
  "elinkUserId": "elink-test-001",
  "userPhone": "13800000001",
  "userEmail": "tester001@example.com",
  "priority": "NORMAL",
  "bizId": "biz-sync-20260710-001"
}
```

### 7.2 异步单条推送

- 方法：`POST`
- 地址：`/api/message-center/push/async`

请求体同同步单条推送，可把 `bizId` 调整为唯一值：

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "registerCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "receiveCorpId": "CSG-GZ-001",
  "sceneParams": {
    "merchantName": "测试食堂",
    "amount": 12.5
  },
  "userId": "emp-test-001",
  "userName": "测试员工",
  "userOrgId": "CSG-GZ-001",
  "userOrgName": "广州测试单位",
  "title": "异步消费通知",
  "content": "异步推送测试内容",
  "priority": "HIGH",
  "bizId": "biz-async-20260710-001"
}
```

### 7.3 同步群发

- 方法：`POST`
- 地址：`/api/message-center/push/sync/mass`

```json
{
  "userId": "system",
  "registerCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "receiveCorpId": "CSG-GZ-001",
  "data": {
    "sceneParams": {
      "merchantName": "测试食堂",
      "amount": 12.5
    },
    "title": "群发消费通知",
    "content": "群发测试内容",
    "url": "https://example.com/message/detail/mass"
  },
  "recipients": [
    {
      "userId": "emp-test-001",
      "userName": "测试员工1",
      "userOrgId": "CSG-GZ-001",
      "userOrgName": "广州测试单位",
      "userPhone": "13800000001",
      "userEmail": "tester001@example.com"
    },
    {
      "userId": "emp-test-002",
      "userName": "测试员工2",
      "userOrgId": "CSG-GZ-001",
      "userOrgName": "广州测试单位",
      "userPhone": "13800000002",
      "userEmail": "tester002@example.com"
    }
  ],
  "registerXtbs": "CANTEEN_CONSUME",
  "priority": "NORMAL",
  "bizId": "biz-mass-20260710-001"
}
```

### 7.4 同步组发

- 方法：`POST`
- 地址：`/api/message-center/push/sync/group`

```json
{
  "userId": "system",
  "registerCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "receiveCorpId": "CSG-GZ-001",
  "title": "组发消费通知",
  "url": "https://example.com/message/detail/group",
  "messages": [
    {
      "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
      "sceneParams": {
        "merchantName": "测试食堂A",
        "amount": 10.5
      },
      "recipient": {
        "userId": "emp-test-001",
        "userName": "测试员工1",
        "userOrgId": "CSG-GZ-001",
        "userOrgName": "广州测试单位",
        "userPhone": "13800000001",
        "userEmail": "tester001@example.com"
      },
      "title": "员工1消费通知"
    },
    {
      "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
      "sceneParams": {
        "merchantName": "测试食堂B",
        "amount": 20
      },
      "recipient": {
        "userId": "emp-test-002",
        "userName": "测试员工2",
        "userOrgId": "CSG-GZ-001",
        "userOrgName": "广州测试单位",
        "userPhone": "13800000002",
        "userEmail": "tester002@example.com"
      },
      "title": "员工2消费通知"
    }
  ],
  "priority": "NORMAL",
  "bizId": "biz-group-20260710-001"
}
```

## 8. 消息记录

### 8.1 消息记录概览

- 方法：`GET`
- 地址：`/api/msg/record/overview`

### 8.2 消息记录分页查询

- 方法：`GET`
- 地址：`/api/msg/record/list`

```http
GET /api/msg/record/list?pageNum=1&pageSize=10&sceneCode=CANTEEN_CONSUME_SUCCESS_TEST&channelType=IN_APP&sendStatus=SUCCESS&priority=NORMAL&callType=SYNC&userId=emp-test-001&userOrgId=CSG-GZ-001
```

可选 Query：

| 参数 | 示例 | 说明 |
| --- | --- | --- |
| msgId | `MC202607100001` | 消息 ID，精确匹配 |
| bizId | `biz-sync-20260710-001` | 业务幂等 ID |
| sceneCode | `CANTEEN_CONSUME_SUCCESS_TEST` | 场景编码 |
| channelType | `IN_APP` | 渠道类型 |
| channelId | `{channelId}` | 渠道 ID |
| channelName | `站内信` | 渠道名称，模糊匹配 |
| templateId | `{templateId}` | 模板 ID |
| templateName | `食堂消费` | 模板名称，模糊匹配 |
| sendStatus | `SUCCESS` | `SUCCESS`、`FAILED`、`PENDING`、`ACCEPTED` |
| priority | `NORMAL` | `HIGH`、`NORMAL`、`LOW` |
| callType | `SYNC` | `SYNC`、`ASYNC` |
| userId | `emp-test-001` | 用户 ID |
| userOrgId | `CSG-GZ-001` | 用户单位 ID |
| startTime | `2026-07-10 00:00:00` | 发送开始时间 |
| endTime | `2026-07-10 23:59:59` | 发送结束时间 |

### 8.3 消息记录详情

- 方法：`GET`
- 地址：`/api/msg/record/{recordId}`

### 8.4 失败记录手工重发

- 方法：`POST`
- 地址：`/api/msg/record/{recordId}/resend`
- 说明：仅失败记录可重发。

### 8.5 查询手工重发日志

- 方法：`GET`
- 地址：`/api/msg/record/{recordId}/resend-logs`

### 8.6 导出消息记录

- 方法：`GET`
- 地址：`/api/msg/record/export`

```http
GET /api/msg/record/export?sceneCode=CANTEEN_CONSUME_SUCCESS_TEST&channelType=IN_APP&startTime=2026-07-10%2000:00:00&endTime=2026-07-10%2023:59:59
```

响应为 `.xlsx` 文件流。

## 9. 消息统计

统计基于消息记录，时间筛选使用 `send_time`。常用筛选字段：

| 参数 | 示例 | 说明 |
| --- | --- | --- |
| startTime | `2026-07-10 00:00:00` | 统计开始时间 |
| endTime | `2026-07-10 23:59:59` | 统计结束时间 |
| channelTypes | `IN_APP` | 可传多个 |
| sceneIds | `{sceneId}` | 可传多个 |
| unitIds | `CSG-GZ-001` | 可传多个 |
| templateIds | `{templateId}` | 可传多个 |
| callTypes | `SYNC` | 可传 `SYNC`、`ASYNC` |

数组类 Query 测试时可按以下形式传递：

```http
channelTypes=IN_APP&channelTypes=SMS&unitIds=CSG-GZ-001
```

### 9.1 统计总览

- 方法：`GET`
- 地址：`/api/msg/statistics/overview`

```http
GET /api/msg/statistics/overview?startTime=2026-07-10%2000:00:00&endTime=2026-07-10%2023:59:59&channelTypes=IN_APP&unitIds=CSG-GZ-001
```

### 9.2 按时间统计

- 方法：`GET`
- 地址：`/api/msg/statistics/time`

```http
GET /api/msg/statistics/time?granularity=DAY&startTime=2026-07-01%2000:00:00&endTime=2026-07-10%2023:59:59&channelTypes=IN_APP
```

`granularity`：`DAY`、`WEEK`、`MONTH`。

### 9.3 按渠道统计

- 方法：`GET`
- 地址：`/api/msg/statistics/channel`

```http
GET /api/msg/statistics/channel?startTime=2026-07-10%2000:00:00&endTime=2026-07-10%2023:59:59
```

### 9.4 按场景统计

- 方法：`GET`
- 地址：`/api/msg/statistics/scene`

```http
GET /api/msg/statistics/scene?sceneIds={sceneId}&startTime=2026-07-10%2000:00:00&endTime=2026-07-10%2023:59:59
```

### 9.5 按单位统计

- 方法：`GET`
- 地址：`/api/msg/statistics/unit`

```http
GET /api/msg/statistics/unit?unitIds=CSG-GZ-001&startTime=2026-07-10%2000:00:00&endTime=2026-07-10%2023:59:59
```

### 9.6 按模板统计

- 方法：`GET`
- 地址：`/api/msg/statistics/template`

```http
GET /api/msg/statistics/template?templateIds={templateId}&startTime=2026-07-10%2000:00:00&endTime=2026-07-10%2023:59:59
```

### 9.7 导出统计报表

- 方法：`GET`
- 地址：`/api/msg/statistics/export`

```http
GET /api/msg/statistics/export?dimension=TIME&scope=CURRENT&granularity=DAY&startTime=2026-07-01%2000:00:00&endTime=2026-07-10%2023:59:59
```

`dimension`：`TIME`、`CHANNEL`、`SCENE`、`UNIT`、`TEMPLATE`。

`scope`：`CURRENT` 当前筛选条件，`ALL` 忽略维度筛选但保留时间范围。

响应为 `.xlsx` 文件流。

## 10. 组织机构

### 10.1 查询组织树

- 方法：`GET`
- 地址：`/api/msg/organization/tree`

## 11. 常见异常测试数据

### 11.1 缺少必填参数

```json
{
  "sceneCode": "",
  "sceneName": "",
  "module": ""
}
```

预期：返回参数错误，`code` 通常为 `A0400`。

### 11.2 分页参数非法

```http
GET /api/msg/scene/list?pageNum=0&pageSize=101
```

预期：返回参数错误。

### 11.3 重复场景编码

连续两次调用新增场景接口，使用相同 `sceneCode`：

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "sceneName": "重复编码测试",
  "module": "CANTEEN_CONSUME",
  "status": 1
}
```

预期：返回数据重复或业务处理失败。

### 11.4 推送缺少接收人

```json
{
  "sceneCode": "CANTEEN_CONSUME_SUCCESS_TEST",
  "sceneParams": {
    "merchantName": "测试食堂",
    "amount": 12.5
  },
  "userId": "",
  "userOrgId": ""
}
```

预期：返回参数错误。

## 12. 测试清理建议

清理测试数据时建议按依赖反向处理：

1. 删除或停用模板：`DELETE /api/msg/template/{templateId}`。
2. 删除或停用渠道：`DELETE /api/msg/channel/{channelId}`。
3. 删除场景参数：`DELETE /api/msg/scene/{sceneId}/params/{paramId}`。
4. 删除或停用场景：`DELETE /api/msg/scene/{sceneId}`。

消息记录和统计数据一般作为测试留痕，不建议直接删除。
