package org.lml.billing.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分余额响应。
 */
@Data
public class BillingBalanceResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private BigDecimal balance;

    private BigDecimal frozenBalance;
}
