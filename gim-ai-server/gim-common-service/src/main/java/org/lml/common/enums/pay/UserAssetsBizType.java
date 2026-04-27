package org.lml.common.enums.pay;

/**
 * 用户资产流水业务类型。
 * 充值链路会用它做到账流水的幂等和审计分类。
 */
public enum UserAssetsBizType {
    RECHARGE,
    REFUND,
    CONSUME
}
