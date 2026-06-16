package com.csg.ecard.messagecenter.common.constant;

/**
 * 消息中心公共常量。
 */
public final class MessageCenterConstants {

    public static final String TABLE_PREFIX = "msg_";
    public static final String MESSAGE_ID_PREFIX = "MSG_";
    public static final long DEFAULT_PAGE_NO = 1L;
    public static final long DEFAULT_PAGE_SIZE = 10L;
    public static final long MAX_PAGE_SIZE = 100L;
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final int SCENE_CODE_MAX_LENGTH = 64;
    public static final int SCENE_NAME_MAX_LENGTH = 50;
    public static final int PARAM_NAME_MAX_LENGTH = 64;
    public static final int TEMPLATE_NAME_MAX_LENGTH = 50;
    public static final int CHANNEL_NAME_MAX_LENGTH = 50;
    public static final int DESCRIPTION_MAX_LENGTH = 200;
    public static final int BLOCKLY_JSON_MAX_SIZE = 1024 * 1024;
    public static final String IDEMPOTENT_KEY_PREFIX = "msg:idempotent:";
    public static final String ORG_CACHE_KEY_PREFIX = "msg:org:";
    public static final String SCENE_CACHE_KEY_PREFIX = "msg:scene:";
    public static final String TEMPLATE_CACHE_KEY_PREFIX = "msg:template:";
    public static final String CHANNEL_CACHE_KEY_PREFIX = "msg:channel:";

    private MessageCenterConstants() {
    }
}
