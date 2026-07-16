ALTER TABLE msg_record
    ADD user_name VARCHAR(128);

COMMENT
ON COLUMN msg_record.user_name IS '用户姓名';
