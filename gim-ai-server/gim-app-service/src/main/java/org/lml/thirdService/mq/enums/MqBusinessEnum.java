package org.lml.thirdService.mq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MqBusinessEnum {

    /**
     * 外部 API 异步处理通用 Topic
     */
    TEXT_TO_IMAGE("TEXT_TO_IMAGE", "文生图业务"),

//    TEXT_TO_IMAGE

    QUERY_IMAGE_RESULT("QUERY_RESULT","查询生图结果"),

    TEXT_TO_VIDEO("TEXT_TO_VIDEO", "文生视频业务"),

    QUERY_TEXT_TO_VIDEO("QUERY_TEXT_TO_VIDEO_RESULT", "查询文生视频业务"),

    PAY_CANCEL_REQUESTED("PAY_CANCEL_REQUESTED", "支付取消请求"),

    RECHARGE_CREDIT("RECHARGE_CREDIT", "充值积分到账"),
    ;


    private final String code;
    private final String desc;
}
