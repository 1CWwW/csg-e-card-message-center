-- 将消息记录发送状态从 SENDING 统一迁移为 ACCEPTED。
-- 适用于已建库环境；执行前请确认当前约束名称仍为 ck_msg_record_send_status。

ALTER TABLE msg_record DROP CONSTRAINT ck_msg_record_send_status;

UPDATE msg_record
SET send_status = 'ACCEPTED'
WHERE send_status = 'SENDING';

ALTER TABLE msg_record
    ADD CONSTRAINT ck_msg_record_send_status
        CHECK (send_status IN ('PENDING', 'ACCEPTED', 'SUCCESS', 'FAILED'));

COMMENT
ON COLUMN msg_record.send_status IS '发送状态：SUCCESS成功、FAILED失败、PENDING待发送、ACCEPTED已受理';

COMMIT;
