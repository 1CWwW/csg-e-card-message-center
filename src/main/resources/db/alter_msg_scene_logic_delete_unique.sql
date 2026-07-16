ALTER TABLE msg_scene DROP CONSTRAINT uk_msg_scene_code;

ALTER TABLE msg_scene MODIFY deleted BIGINT DEFAULT 0 NOT NULL;

UPDATE msg_scene SET deleted = id WHERE deleted = 1;

ALTER TABLE msg_scene
    ADD CONSTRAINT uk_msg_scene_code_deleted UNIQUE (scene_code, deleted);

ALTER TABLE msg_scene_param DROP CONSTRAINT uk_msg_scene_param_name;

ALTER TABLE msg_scene_param MODIFY deleted BIGINT DEFAULT 0 NOT NULL;

UPDATE msg_scene_param SET deleted = id WHERE deleted = 1;

ALTER TABLE msg_scene_param
    ADD CONSTRAINT uk_msg_scene_param_name_deleted UNIQUE (scene_id, param_name, deleted);
