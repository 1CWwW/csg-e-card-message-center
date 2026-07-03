-- 消息记录与手动重发日志联调示例数据。
-- 仅用于本地/测试环境页面联调；执行前请确认 ID 与现有数据不冲突。
-- 本脚本不会被应用自动执行。

INSERT INTO msg_record (
    id, pc_id, msg_id, biz_id, scene_code, register_code, register_name, register_xtbs,
    msg_type, notice_type, title, url, schedule_time, template_id, channel_id,
    scene_params, message_content, user_id, user_org_id, receive_user_id, receive_corp_id,
    receive_phone, receive_email, email_id, sender_email, sender_email_password,
    sender_email_url, copy_email, file, sender_user_id, elink_user_id,
    priority, call_type, send_status, resend_count, max_resend_count,
    error_msg, error_stack, send_time, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000000001, 'PC-SAMPLE-001', 'MSG-SAMPLE-001', 'BIZ-SAMPLE-SUCCESS-SMS',
    'CANTEEN_CONSUME_SUCCESS', 'CANTEEN_CONSUME_SUCCESS', '食堂消费成功通知', 'ecard',
    'sms', NULL, NULL, NULL, NULL, 910000000000100001, 910000000000200001,
    '{"amount":18.50,"merchantName":"第一食堂","consumeTime":"2026-07-03 09:10:00"}',
    '您在第一食堂消费18.50元。', 'U10001', 'ORG001', 'U10001', 'ORG001',
    '13800000001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', NULL,
    'NORMAL', 'SYNC', 'SUCCESS', 0, 5, NULL, NULL,
    TIMESTAMP '2026-07-03 09:10:05', 'system', TIMESTAMP '2026-07-03 09:10:00',
    'system', TIMESTAMP '2026-07-03 09:10:05', 0
);

INSERT INTO msg_record (
    id, pc_id, msg_id, biz_id, scene_code, register_code, register_name, register_xtbs,
    msg_type, notice_type, title, url, schedule_time, template_id, channel_id,
    scene_params, message_content, user_id, user_org_id, receive_user_id, receive_corp_id,
    receive_phone, receive_email, email_id, sender_email, sender_email_password,
    sender_email_url, copy_email, file, sender_user_id, elink_user_id,
    priority, call_type, send_status, resend_count, max_resend_count,
    error_msg, error_stack, send_time, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000000002, 'PC-SAMPLE-002', 'MSG-SAMPLE-002', 'BIZ-SAMPLE-FAILED-EMAIL',
    'NOTICE_APPROVAL', 'NOTICE_APPROVAL', '审批通知', 'ecard',
    'email', NULL, '审批待处理提醒', NULL, NULL, 910000000000100002, 910000000000200002,
    '{"applyNo":"AP20260703001","applyUser":"张三","deadline":"2026-07-05"}',
    '您有一条审批单 AP20260703001 待处理。', 'U10002', 'ORG002', 'U10002', 'ORG002',
    NULL, 'user10002@example.com', 'EMAIL-SAMPLE-002', 'sender@example.com',
    '***', 'smtp.example.com', 'leader@example.com', '[]', 'admin', NULL,
    'HIGH', 'ASYNC', 'FAILED', 2, 5,
    'SMTP连接超时', 'java.net.SocketTimeoutException: SMTP连接超时',
    TIMESTAMP '2026-07-03 09:35:30', 'system', TIMESTAMP '2026-07-03 09:30:00',
    'system', TIMESTAMP '2026-07-03 09:35:30', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010001, 910000000000000002, 1, 'FAILED',
    'SMTP连接超时', 'java.net.SocketTimeoutException: SMTP连接超时',
    TIMESTAMP '2026-07-03 09:32:00', TIMESTAMP '2026-07-03 09:32:06',
    'admin', 'admin', TIMESTAMP '2026-07-03 09:32:06',
    'admin', TIMESTAMP '2026-07-03 09:32:06', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010002, 910000000000000002, 2, 'FAILED',
    'SMTP连接超时', 'java.net.SocketTimeoutException: SMTP连接超时',
    TIMESTAMP '2026-07-03 09:35:20', TIMESTAMP '2026-07-03 09:35:30',
    'admin', 'admin', TIMESTAMP '2026-07-03 09:35:30',
    'admin', TIMESTAMP '2026-07-03 09:35:30', 0
);

INSERT INTO msg_record (
    id, pc_id, msg_id, biz_id, scene_code, register_code, register_name, register_xtbs,
    msg_type, notice_type, title, url, schedule_time, template_id, channel_id,
    scene_params, message_content, user_id, user_org_id, receive_user_id, receive_corp_id,
    receive_phone, receive_email, email_id, sender_email, sender_email_password,
    sender_email_url, copy_email, file, sender_user_id, elink_user_id,
    priority, call_type, send_status, resend_count, max_resend_count,
    error_msg, error_stack, send_time, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000000003, 'PC-SAMPLE-003', 'MSG-SAMPLE-003', 'BIZ-SAMPLE-RESEND-SUCCESS',
    'BALANCE_WARNING', 'BALANCE_WARNING', '余额不足提醒', 'ecard',
    'elink', NULL, '余额不足提醒', 'https://example.com/balance', NULL,
    910000000000100003, 910000000000200003,
    '{"balance":8.00,"threshold":10.00}', '您的余额不足10元，请及时充值。',
    'U10003', 'ORG003', 'U10003', 'ORG003', NULL, NULL, NULL, NULL, NULL, NULL,
    NULL, NULL, 'admin', 'elink_u10003',
    'NORMAL', 'SYNC', 'SUCCESS', 1, 5, NULL, NULL,
    TIMESTAMP '2026-07-03 10:05:10', 'system', TIMESTAMP '2026-07-03 10:00:00',
    'admin', TIMESTAMP '2026-07-03 10:05:10', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010003, 910000000000000003, 1, 'SUCCESS',
    NULL, NULL, TIMESTAMP '2026-07-03 10:05:00', TIMESTAMP '2026-07-03 10:05:10',
    'admin', 'admin', TIMESTAMP '2026-07-03 10:05:10',
    'admin', TIMESTAMP '2026-07-03 10:05:10', 0
);

INSERT INTO msg_record (
    id, pc_id, msg_id, biz_id, scene_code, register_code, register_name, register_xtbs,
    msg_type, notice_type, title, url, schedule_time, template_id, channel_id,
    scene_params, message_content, user_id, user_org_id, receive_user_id, receive_corp_id,
    receive_phone, receive_email, email_id, sender_email, sender_email_password,
    sender_email_url, copy_email, file, sender_user_id, elink_user_id,
    priority, call_type, send_status, resend_count, max_resend_count,
    error_msg, error_stack, send_time, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000000004, 'PC-SAMPLE-004', 'MSG-SAMPLE-004', 'BIZ-SAMPLE-MAX-RESEND',
    'CARD_LOSS_NOTICE', 'CARD_LOSS_NOTICE', '卡片挂失通知', 'ecard',
    'sms', NULL, NULL, NULL, NULL, 910000000000100004, 910000000000200004,
    '{"cardNo":"622200****0004","lossTime":"2026-07-03 10:20:00"}',
    '您的卡片已挂失，如非本人操作请联系管理员。', 'U10004', 'ORG004', 'U10004', 'ORG004',
    '13800000004', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', NULL,
    'HIGH', 'SYNC', 'FAILED', 5, 5,
    '短信网关返回失败', 'com.example.SmsGatewayException: provider rejected',
    TIMESTAMP '2026-07-03 10:30:00', 'system', TIMESTAMP '2026-07-03 10:20:00',
    'admin', TIMESTAMP '2026-07-03 10:30:00', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010004, 910000000000000004, 1, 'FAILED',
    '短信网关返回失败', 'com.example.SmsGatewayException: provider rejected',
    TIMESTAMP '2026-07-03 10:22:00', TIMESTAMP '2026-07-03 10:22:03',
    'admin', 'admin', TIMESTAMP '2026-07-03 10:22:03',
    'admin', TIMESTAMP '2026-07-03 10:22:03', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010005, 910000000000000004, 2, 'FAILED',
    '短信网关返回失败', 'com.example.SmsGatewayException: provider rejected',
    TIMESTAMP '2026-07-03 10:24:00', TIMESTAMP '2026-07-03 10:24:04',
    'admin', 'admin', TIMESTAMP '2026-07-03 10:24:04',
    'admin', TIMESTAMP '2026-07-03 10:24:04', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010006, 910000000000000004, 3, 'FAILED',
    '短信网关返回失败', 'com.example.SmsGatewayException: provider rejected',
    TIMESTAMP '2026-07-03 10:26:00', TIMESTAMP '2026-07-03 10:26:05',
    'admin', 'admin', TIMESTAMP '2026-07-03 10:26:05',
    'admin', TIMESTAMP '2026-07-03 10:26:05', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010007, 910000000000000004, 4, 'FAILED',
    '短信网关返回失败', 'com.example.SmsGatewayException: provider rejected',
    TIMESTAMP '2026-07-03 10:28:00', TIMESTAMP '2026-07-03 10:28:04',
    'admin', 'admin', TIMESTAMP '2026-07-03 10:28:04',
    'admin', TIMESTAMP '2026-07-03 10:28:04', 0
);

INSERT INTO msg_record_resend_log (
    id, record_id, resend_no, send_status, error_msg, error_stack,
    start_time, end_time, operator_id, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000010008, 910000000000000004, 5, 'FAILED',
    '短信网关返回失败', 'com.example.SmsGatewayException: provider rejected',
    TIMESTAMP '2026-07-03 10:30:00', TIMESTAMP '2026-07-03 10:30:05',
    'admin', 'admin', TIMESTAMP '2026-07-03 10:30:05',
    'admin', TIMESTAMP '2026-07-03 10:30:05', 0
);

INSERT INTO msg_record (
    id, pc_id, msg_id, biz_id, scene_code, register_code, register_name, register_xtbs,
    msg_type, notice_type, title, url, schedule_time, template_id, channel_id,
    scene_params, message_content, user_id, user_org_id, receive_user_id, receive_corp_id,
    receive_phone, receive_email, email_id, sender_email, sender_email_password,
    sender_email_url, copy_email, file, sender_user_id, elink_user_id,
    priority, call_type, send_status, resend_count, max_resend_count,
    error_msg, error_stack, send_time, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000000005, 'PC-SAMPLE-005', 'MSG-SAMPLE-005', 'BIZ-SAMPLE-PENDING-INAPP',
    'MEETING_REMINDER', 'MEETING_REMINDER', '会议提醒', 'ecard',
    'sym', 'NOTICE', '会议提醒', 'https://example.com/meeting/1005',
    TIMESTAMP '2026-07-03 18:00:00', 910000000000100005, 910000000000200005,
    '{"meetingName":"项目周会","meetingTime":"2026-07-03 18:00:00"}',
    '项目周会将于18:00开始。', 'U10005', 'ORG005', 'U10005', 'ORG005',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', NULL,
    'LOW', 'ASYNC', 'PENDING', 0, 5, NULL, NULL, NULL,
    'system', TIMESTAMP '2026-07-03 11:00:00',
    'system', TIMESTAMP '2026-07-03 11:00:00', 0
);

INSERT INTO msg_record (
    id, pc_id, msg_id, biz_id, scene_code, register_code, register_name, register_xtbs,
    msg_type, notice_type, title, url, schedule_time, template_id, channel_id,
    scene_params, message_content, user_id, user_org_id, receive_user_id, receive_corp_id,
    receive_phone, receive_email, email_id, sender_email, sender_email_password,
    sender_email_url, copy_email, file, sender_user_id, elink_user_id,
    priority, call_type, send_status, resend_count, max_resend_count,
    error_msg, error_stack, send_time, create_by, create_time, update_by, update_time, deleted
) VALUES (
    910000000000000006, 'PC-SAMPLE-006', 'MSG-SAMPLE-006', 'BIZ-SAMPLE-ACCEPTED-EMAIL',
    'MONTHLY_REPORT', 'MONTHLY_REPORT', '月报发送', 'ecard',
    'email', NULL, '月报通知', NULL, NULL, 910000000000100006, 910000000000200006,
    '{"month":"2026-06","reportName":"六月运营月报"}',
    '六月运营月报已生成，请查收。', 'U10006', 'ORG006', 'U10006', 'ORG006',
    NULL, 'user10006@example.com', 'EMAIL-SAMPLE-006', 'sender@example.com',
    '***', 'smtp.example.com', NULL, '[]', 'admin', NULL,
    'NORMAL', 'ASYNC', 'ACCEPTED', 0, 5, NULL, NULL, NULL,
    'system', TIMESTAMP '2026-07-03 11:20:00',
    'system', TIMESTAMP '2026-07-03 11:20:00', 0
);

COMMIT;
