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
    id          BIGINT      NOT NULL,
    scene_id    BIGINT      NOT NULL,
    param_name  VARCHAR(64) NOT NULL,
    param_label VARCHAR(20) NOT NULL,
    param_type  VARCHAR(32) NOT NULL,
    sort_order  INT         NOT NULL,
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
