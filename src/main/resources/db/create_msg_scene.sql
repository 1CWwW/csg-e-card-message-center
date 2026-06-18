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
    deleted     INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_msg_scene PRIMARY KEY (id),
    CONSTRAINT uk_msg_scene_code UNIQUE (scene_code)
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
ON COLUMN msg_scene.deleted IS '逻辑删除标记，0正常，1删除';

CREATE INDEX idx_msg_scene_module_status ON msg_scene (module, status);
CREATE INDEX idx_msg_scene_deleted_create_time ON msg_scene (deleted, create_time);

CREATE TABLE msg_scene_param
(
    id          BIGINT        NOT NULL,
    scene_id    BIGINT        NOT NULL,
    param_name  VARCHAR(64)   NOT NULL,
    param_label VARCHAR(20)   NOT NULL,
    param_type  VARCHAR(32)   NOT NULL,
    sort_order  INT           NOT NULL,
    is_required INT DEFAULT 0 NOT NULL,
    create_by   VARCHAR(64),
    create_time TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP,
    deleted     INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_msg_scene_param PRIMARY KEY (id),
    CONSTRAINT uk_msg_scene_param_name UNIQUE (scene_id, param_name)
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
ON COLUMN msg_scene_param.deleted IS '逻辑删除标记，0正常，1删除';

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
