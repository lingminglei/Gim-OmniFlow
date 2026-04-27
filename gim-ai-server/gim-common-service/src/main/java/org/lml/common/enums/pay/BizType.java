package org.lml.common.enums.pay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务场景枚举
 */
@Getter
@AllArgsConstructor
public enum BizType {

    TRY_PAY("TRY_PAY", "尝试支付"),

    CONFIRM_PAY("CONFIRM_PAY", "确认支付")

    ;


    /**
     * 业务类型唯一标识
     *
     * 对应数据库 transaction_logs 表中的 biz_type 字段
     */
    private final String bizType;

    /**
     * 基础消耗积分数值
     * 作为计费策略中的核心参数
     */
    private final String message;
}
