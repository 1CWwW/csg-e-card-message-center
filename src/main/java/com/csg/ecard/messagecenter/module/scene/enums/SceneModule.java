package com.csg.ecard.messagecenter.module.scene.enums;

import java.util.Arrays;

/**
 * 场景所属模块枚举。
 */
public enum SceneModule {

    CANTEEN_CONSUME("CANTEEN_CONSUME", "食堂消费"),
    HERTZ_SHOPPING("HERTZ_SHOPPING", "赫兹乐购"),
    ACCESS_CHECK("ACCESS_CHECK", "门禁核验"),
    COMPLAINT_FEEDBACK("COMPLAINT_FEEDBACK", "投诉反馈"),
    PROCESS_APPROVAL("PROCESS_APPROVAL", "流程审批"),
    ORDER_MANAGEMENT("ORDER_MANAGEMENT", "订单管理"),
    ACCOUNT_MANAGEMENT("ACCOUNT_MANAGEMENT", "账户管理"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    SceneModule(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取场景所属模块。
     *
     * @param code 模块编码
     * @return 场景所属模块
     */
    public static SceneModule fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知场景所属模块: " + code));
    }
}
