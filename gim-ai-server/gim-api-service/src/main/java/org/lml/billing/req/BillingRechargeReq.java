package org.lml.billing.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 积分充值请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BillingRechargeReq extends BillingBaseChangeReq {

    private static final long serialVersionUID = 1L;
}
