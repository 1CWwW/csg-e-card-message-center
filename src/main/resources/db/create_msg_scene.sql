CREATE TABLE msg_scene
(
    id          BIGINT        NOT NULL,
    scene_code  VARCHAR(64)   NOT NULL,
    scene_name  VARCHAR(50)   NOT NULL,
    module      VARCHAR(64)   NOT NULL,
    description VARCHAR(200),
    status      INT DEFAULT 1 NOT NULL,
    create_by   VARCHAR(64),
    create_time TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP,
    deleted     BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_msg_scene PRIMARY KEY (id),
    CONSTRAINT uk_msg_scene_code_deleted UNIQUE (scene_code, deleted)
);

COMMENT
ON TABLE msg_scene IS '消息场景表';
COMMENT
ON COLUMN msg_scene.id IS '主键ID';
COMMENT
ON COLUMN msg_scene.scene_code IS '场景编码';
COMMENT
ON COLUMN msg_scene.scene_name IS '场景名称';
COMMENT
ON COLUMN msg_scene.module IS '所属模块';
COMMENT
ON COLUMN msg_scene.description IS '场景描述';
COMMENT
ON COLUMN msg_scene.status IS '启用状态';
COMMENT
ON COLUMN msg_scene.create_by IS '创建人';
COMMENT
ON COLUMN msg_scene.create_time IS '创建时间';
COMMENT
ON COLUMN msg_scene.update_by IS '更新人';
COMMENT
ON COLUMN msg_scene.update_time IS '更新时间';
COMMENT
ON COLUMN msg_scene.deleted IS '逻辑删除标记，0正常，非0删除；删除时保存场景ID以支持同编码重建';

CREATE INDEX idx_msg_scene_module_status ON msg_scene (module, status);
CREATE INDEX idx_msg_scene_deleted_create_time ON msg_scene (deleted, create_time);

CREATE TABLE msg_scene_param
(
    id          BIGINT        NOT NULL,
    scene_id    BIGINT        NOT NULL,
    param_name  VARCHAR(64)   NOT NULL,
    param_label VARCHAR(200)  NOT NULL,
    param_type  VARCHAR(32)   NOT NULL,
    sort_order  INT           NOT NULL,
    is_required INT DEFAULT 0 NOT NULL,
    create_by   VARCHAR(64),
    create_time TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP,
    deleted     BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_msg_scene_param PRIMARY KEY (id),
    CONSTRAINT uk_msg_scene_param_name_deleted UNIQUE (scene_id, param_name, deleted)
);

COMMENT
ON TABLE msg_scene_param IS '场景参数表';
COMMENT
ON COLUMN msg_scene_param.id IS '主键ID';
COMMENT
ON COLUMN msg_scene_param.scene_id IS '场景ID';
COMMENT
ON COLUMN msg_scene_param.param_name IS '参数名';
COMMENT
ON COLUMN msg_scene_param.param_label IS '参数标签';
COMMENT
ON COLUMN msg_scene_param.param_type IS '参数类型';
COMMENT
ON COLUMN msg_scene_param.sort_order IS '排序号';
COMMENT
ON COLUMN msg_scene_param.is_required IS '是否必填，0否，1是';
COMMENT
ON COLUMN msg_scene_param.create_by IS '创建人';
COMMENT
ON COLUMN msg_scene_param.create_time IS '创建时间';
COMMENT
ON COLUMN msg_scene_param.update_by IS '更新人';
COMMENT
ON COLUMN msg_scene_param.update_time IS '更新时间';
COMMENT
ON COLUMN msg_scene_param.deleted IS '逻辑删除标记，0正常，非0删除；删除时保存参数ID以支持同名参数重建';

CREATE INDEX idx_msg_scene_param_scene_id ON msg_scene_param (scene_id);
CREATE INDEX idx_msg_scene_param_scene_sort ON msg_scene_param (scene_id, sort_order);

CREATE TABLE msg_channel
(
    id           BIGINT                              NOT NULL,
    channel_name VARCHAR(50)                         NOT NULL,
    channel_type VARCHAR(32)                         NOT NULL,
    type_config  CLOB,
    priority     INT                                 NOT NULL,
    status       INT       DEFAULT 1                 NOT NULL,
    create_by    VARCHAR(64),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by    VARCHAR(64),
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted      INT       DEFAULT 0                 NOT NULL,

    CONSTRAINT pk_msg_channel
        PRIMARY KEY (id),

    CONSTRAINT ck_msg_channel_priority
        CHECK (priority > 0),

    CONSTRAINT ck_msg_channel_status
        CHECK (status IN (0, 1)),

    CONSTRAINT ck_msg_channel_deleted
        CHECK (deleted IN (0, 1))

);

COMMENT
ON TABLE msg_channel IS '消息渠道表';

COMMENT
ON COLUMN msg_channel.id IS '主键ID';
COMMENT
ON COLUMN msg_channel.channel_name IS '渠道名称';
COMMENT
ON COLUMN msg_channel.channel_type IS '渠道类型：SMS、EMAIL、ELINK、IN_APP';
COMMENT
ON COLUMN msg_channel.type_config IS '渠道类型配置JSON';
COMMENT
ON COLUMN msg_channel.priority IS '优先级，数字越小优先级越高';
COMMENT
ON COLUMN msg_channel.status IS '启停状态，1启用，0停用';
COMMENT
ON COLUMN msg_channel.create_by IS '创建人';
COMMENT
ON COLUMN msg_channel.create_time IS '创建时间';
COMMENT
ON COLUMN msg_channel.update_by IS '更新人';
COMMENT
ON COLUMN msg_channel.update_time IS '更新时间';
COMMENT
ON COLUMN msg_channel.deleted IS '逻辑删除标记，0正常，1删除';

CREATE INDEX idx_msg_channel_name ON msg_channel (channel_name);

CREATE INDEX idx_msg_channel_status ON msg_channel (status);

CREATE INDEX idx_msg_channel_type_status_priority ON msg_channel (channel_type, status, priority, create_time, id);

CREATE TABLE msg_channel_unit
(
    id          BIGINT      NOT NULL,
    channel_id  BIGINT      NOT NULL,
    unit_id     VARCHAR(64) NOT NULL,
    create_time TIMESTAMP,
    create_by   VARCHAR(64),
    CONSTRAINT pk_msg_channel_unit PRIMARY KEY (id),
    CONSTRAINT uk_msg_channel_unit UNIQUE (channel_id, unit_id)
);

COMMENT
ON TABLE msg_channel_unit IS '渠道适用单位关联表';
COMMENT
ON COLUMN msg_channel_unit.id IS '主键ID';
COMMENT
ON COLUMN msg_channel_unit.channel_id IS '渠道ID';
COMMENT
ON COLUMN msg_channel_unit.unit_id IS '单位ID';
COMMENT
ON COLUMN msg_channel_unit.create_time IS '创建时间';
COMMENT
ON COLUMN msg_channel_unit.create_by IS '创建人';

CREATE INDEX idx_msg_channel_unit_channel_id ON msg_channel_unit (channel_id);
CREATE INDEX idx_msg_channel_unit_unit_id ON msg_channel_unit (unit_id);

CREATE TABLE msg_template
(
    id            BIGINT                              NOT NULL,
    template_name VARCHAR(50)                         NOT NULL,
    scene_id      BIGINT                              NOT NULL,
    channel_type  VARCHAR(32)                         NOT NULL,
    blockly_json  CLOB,
    status        INT       DEFAULT 0                 NOT NULL,
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by     VARCHAR(64),
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted       INT       DEFAULT 0                 NOT NULL,

    CONSTRAINT pk_msg_template
        PRIMARY KEY (id),

    CONSTRAINT ck_msg_template_status
        CHECK (status IN (0, 1)),

    CONSTRAINT ck_msg_template_deleted
        CHECK (deleted IN (0, 1))
);

COMMENT
ON TABLE msg_template IS '消息模板表';
COMMENT
ON COLUMN msg_template.id IS '主键ID';
COMMENT
ON COLUMN msg_template.template_name IS '模板名称';
COMMENT
ON COLUMN msg_template.scene_id IS '场景ID';
COMMENT
ON COLUMN msg_template.channel_type IS '渠道类型：SMS、EMAIL、ELINK、IN_APP';
COMMENT
ON COLUMN msg_template.blockly_json IS 'Blockly内容JSON';
COMMENT
ON COLUMN msg_template.status IS '启停状态，1启用，0停用';
COMMENT
ON COLUMN msg_template.create_by IS '创建人';
COMMENT
ON COLUMN msg_template.create_time IS '创建时间';
COMMENT
ON COLUMN msg_template.update_by IS '更新人';
COMMENT
ON COLUMN msg_template.update_time IS '更新时间';
COMMENT
ON COLUMN msg_template.deleted IS '逻辑删除标记，0正常，1删除';

CREATE INDEX idx_msg_template_scene_id ON msg_template (scene_id);
CREATE INDEX idx_msg_template_channel_type ON msg_template (channel_type);
CREATE INDEX idx_msg_template_status ON msg_template (status);
CREATE INDEX idx_msg_template_scene_name ON msg_template (scene_id, template_name);

CREATE TABLE msg_template_unit
(
    id          BIGINT                              NOT NULL,
    template_id BIGINT                              NOT NULL,
    unit_id     VARCHAR(64)                         NOT NULL,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT pk_msg_template_unit
        PRIMARY KEY (id),

    CONSTRAINT uk_msg_template_unit
        UNIQUE (template_id, unit_id)
);

COMMENT
ON TABLE msg_template_unit IS '模板适用单位关联表';
COMMENT
ON COLUMN msg_template_unit.id IS '主键ID';
COMMENT
ON COLUMN msg_template_unit.template_id IS '模板ID';
COMMENT
ON COLUMN msg_template_unit.unit_id IS '单位ID';
COMMENT
ON COLUMN msg_template_unit.create_by IS '创建人';
COMMENT
ON COLUMN msg_template_unit.create_time IS '创建时间';

CREATE INDEX idx_msg_template_unit_template_id ON msg_template_unit (template_id);
CREATE INDEX idx_msg_template_unit_unit_id ON msg_template_unit (unit_id);

CREATE TABLE msg_record
(
    id              BIGINT                                NOT NULL,
    pc_id           VARCHAR(64)                           NOT NULL,
    msg_id          VARCHAR(64)                           NOT NULL,
    biz_id          VARCHAR(128),
    scene_code      VARCHAR(64)                           NOT NULL,
    register_code   VARCHAR(64),
    register_name   VARCHAR(128),
    register_xtbs   VARCHAR(64),
    msg_type        VARCHAR(32),
    notice_type     VARCHAR(32),
    title           VARCHAR(256),
    url             VARCHAR(1000),
    schedule_time   TIMESTAMP,
    template_id     BIGINT                                NOT NULL,
    channel_id      BIGINT,
    scene_params    CLOB                                  NOT NULL,
    message_content CLOB,
    user_id         VARCHAR(128)                          NOT NULL,
    user_org_id     VARCHAR(128)                          NOT NULL,
    receive_user_id VARCHAR(128),
    receive_corp_id VARCHAR(128),
    receive_phone   VARCHAR(32),
    receive_email   VARCHAR(128),
    email_id        VARCHAR(128),
    sender_email    VARCHAR(128),
    sender_email_password VARCHAR(256),
    sender_email_url VARCHAR(256),
    copy_email      CLOB,
    file            CLOB,
    sender_user_id  VARCHAR(128),
    elink_user_id   VARCHAR(128),
    priority        VARCHAR(16) DEFAULT 'NORMAL'          NOT NULL,
    call_type       VARCHAR(16)                           NOT NULL,
    send_status     VARCHAR(16)                           NOT NULL,
    resend_count    INT         DEFAULT 0                 NOT NULL,
    max_resend_count INT        DEFAULT 5                 NOT NULL,
    error_msg       VARCHAR(1000),
    error_stack     CLOB,
    send_time       TIMESTAMP,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted         INT         DEFAULT 0                 NOT NULL,

    CONSTRAINT pk_msg_record
        PRIMARY KEY (id),

    CONSTRAINT ck_msg_record_send_status
        CHECK (send_status IN ('PENDING', 'ACCEPTED', 'SUCCESS', 'FAILED')),

    CONSTRAINT ck_msg_record_priority
        CHECK (priority IN ('HIGH', 'NORMAL', 'LOW')),

    CONSTRAINT ck_msg_record_call_type
        CHECK (call_type IN ('SYNC', 'ASYNC')),

    CONSTRAINT ck_msg_record_deleted
        CHECK (deleted IN (0, 1))
);

COMMENT
ON TABLE msg_record IS '消息发送记录表';
COMMENT
ON COLUMN msg_record.id IS '主键ID';
COMMENT
ON COLUMN msg_record.biz_id IS '业务幂等ID';
COMMENT
ON COLUMN msg_record.scene_code IS '场景编码';
COMMENT
ON COLUMN msg_record.template_id IS '模板ID';
COMMENT
ON COLUMN msg_record.channel_id IS '渠道ID，未匹配到渠道时为空';
COMMENT
ON COLUMN msg_record.scene_params IS '场景参数JSON';
COMMENT
ON COLUMN msg_record.message_content IS '渲染后的消息正文';
COMMENT
ON COLUMN msg_record.user_id IS '用户ID';
COMMENT
ON COLUMN msg_record.user_org_id IS '用户单位ID';
COMMENT
ON COLUMN msg_record.priority IS '消息业务优先级：HIGH、NORMAL、LOW';
COMMENT
ON COLUMN msg_record.call_type IS '消息原始调用方式：SYNC同步、ASYNC异步';
COMMENT
ON COLUMN msg_record.error_msg IS '发送失败原因';
COMMENT
ON COLUMN msg_record.error_stack IS '最近一次实际发送失败产生的技术异常堆栈';
COMMENT
ON COLUMN msg_record.send_time IS '最近一次实际发送尝试完成时间';
COMMENT
ON COLUMN msg_record.create_by IS '创建人';
COMMENT
ON COLUMN msg_record.create_time IS '创建时间';
COMMENT
ON COLUMN msg_record.update_by IS '更新人';
COMMENT
ON COLUMN msg_record.update_time IS '更新时间';
COMMENT
ON COLUMN msg_record.deleted IS '逻辑删除标记：0正常，1删除';

COMMENT
ON COLUMN msg_record.pc_id IS '消息批次ID，同一次推送共用';
COMMENT
ON COLUMN msg_record.msg_id IS '单条发送消息ID';
COMMENT
ON COLUMN msg_record.register_code IS '消息编码';
COMMENT
ON COLUMN msg_record.register_name IS '消息名称';
COMMENT
ON COLUMN msg_record.register_xtbs IS '消息系统标识';
COMMENT
ON COLUMN msg_record.msg_type IS '消息类型，SMS/EMAIL/ELINK对应小写类型，IN_APP对应sym';
COMMENT
ON COLUMN msg_record.notice_type IS '站内信通知类型';
COMMENT
ON COLUMN msg_record.title IS '邮件/eLink/站内信标题';
COMMENT
ON COLUMN msg_record.url IS '跳转链接';
COMMENT
ON COLUMN msg_record.schedule_time IS '预计发送时间，空表示立即发送';
COMMENT
ON COLUMN msg_record.receive_user_id IS '接收人ID';
COMMENT
ON COLUMN msg_record.receive_corp_id IS '接收人单位ID';
COMMENT
ON COLUMN msg_record.receive_phone IS '接收人手机号';
COMMENT
ON COLUMN msg_record.receive_email IS '接收人邮箱';
COMMENT
ON COLUMN msg_record.sender_user_id IS '发送人ID';
COMMENT
ON COLUMN msg_record.elink_user_id IS 'eLink用户ID';

COMMENT
ON COLUMN msg_record.email_id IS '邮件ID，同一邮件ID的邮件记录聚合为一封邮件发送';
COMMENT
ON COLUMN msg_record.sender_email IS '发送人邮箱';
COMMENT
ON COLUMN msg_record.sender_email_password IS '发送邮箱密码';
COMMENT
ON COLUMN msg_record.sender_email_url IS '发送邮箱服务器';
COMMENT
ON COLUMN msg_record.copy_email IS '邮件抄送邮箱，逗号分隔';
COMMENT
ON COLUMN msg_record.file IS '邮件附件JSON';
COMMENT
ON COLUMN msg_record.send_status IS '发送状态：SUCCESS成功、FAILED失败、PENDING待发送、ACCEPTED已受理';

COMMENT
ON COLUMN msg_record.resend_count IS '手动重发次数';
COMMENT
ON COLUMN msg_record.max_resend_count IS '允许手动重发最大次数';

CREATE INDEX idx_msg_record_pc_id ON msg_record (pc_id);
CREATE INDEX idx_msg_record_pc_template_user ON msg_record (pc_id, template_id, user_id);
CREATE INDEX idx_msg_record_msg_id ON msg_record (msg_id);
CREATE INDEX idx_msg_record_biz_id ON msg_record (biz_id);
CREATE INDEX idx_msg_record_scene_code ON msg_record (scene_code);
CREATE INDEX idx_msg_record_template_id ON msg_record (template_id);
CREATE INDEX idx_msg_record_channel_id ON msg_record (channel_id);
CREATE INDEX idx_msg_record_user_id ON msg_record (user_id);
CREATE INDEX idx_msg_record_user_org_id ON msg_record (user_org_id);
CREATE INDEX idx_msg_record_priority ON msg_record (priority);
CREATE INDEX idx_msg_record_call_type ON msg_record (call_type);
CREATE INDEX idx_msg_record_send_status ON msg_record (send_status);
CREATE INDEX idx_msg_record_send_time ON msg_record (send_time);
CREATE INDEX idx_msg_record_schedule_time ON msg_record (schedule_time);
CREATE INDEX idx_msg_record_pc_msg_type_email ON msg_record (pc_id, msg_type, email_id);

CREATE TABLE msg_record_resend_log
(
    id          BIGINT                                NOT NULL,
    record_id   BIGINT                                NOT NULL,
    resend_no   INT                                   NOT NULL,
    send_status VARCHAR(16)                           NOT NULL,
    error_msg   VARCHAR(1000),
    error_stack CLOB,
    start_time  TIMESTAMP                             NOT NULL,
    end_time    TIMESTAMP                             NOT NULL,
    operator_id VARCHAR(64),
    create_by   VARCHAR(64),
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by   VARCHAR(64),
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted     INT         DEFAULT 0                 NOT NULL,

    CONSTRAINT pk_msg_record_resend_log
        PRIMARY KEY (id),

    CONSTRAINT ck_msg_record_resend_log_status
        CHECK (send_status IN ('SUCCESS', 'FAILED')),

    CONSTRAINT ck_msg_record_resend_log_deleted
        CHECK (deleted IN (0, 1))
);

COMMENT
ON TABLE msg_record_resend_log IS '消息记录手动重发日志表';
COMMENT
ON COLUMN msg_record_resend_log.id IS '主键ID';
COMMENT
ON COLUMN msg_record_resend_log.record_id IS '消息记录ID';
COMMENT
ON COLUMN msg_record_resend_log.resend_no IS '第几次手动重发';
COMMENT
ON COLUMN msg_record_resend_log.send_status IS '本次重发结果：SUCCESS成功、FAILED失败';
COMMENT
ON COLUMN msg_record_resend_log.error_msg IS '本次重发失败原因';
COMMENT
ON COLUMN msg_record_resend_log.error_stack IS '本次重发技术异常堆栈';
COMMENT
ON COLUMN msg_record_resend_log.start_time IS '本次重发开始时间';
COMMENT
ON COLUMN msg_record_resend_log.end_time IS '本次重发完成时间';
COMMENT
ON COLUMN msg_record_resend_log.operator_id IS '手动重发操作人ID';
COMMENT
ON COLUMN msg_record_resend_log.create_by IS '创建人';
COMMENT
ON COLUMN msg_record_resend_log.create_time IS '创建时间';
COMMENT
ON COLUMN msg_record_resend_log.update_by IS '更新人';
COMMENT
ON COLUMN msg_record_resend_log.update_time IS '更新时间';
COMMENT
ON COLUMN msg_record_resend_log.deleted IS '逻辑删除标记：0正常，1删除';

CREATE INDEX idx_msg_record_resend_log_record_id ON msg_record_resend_log (record_id);
CREATE INDEX idx_msg_record_resend_log_record_no ON msg_record_resend_log (record_id, resend_no);
