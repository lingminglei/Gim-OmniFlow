package org.lml.billing.resp;

import lombok.Data;
import org.lml.billing.enums.BillingOperationStatus;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分充值响应。
 */
@Data
public class BillingRechargeResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestNo;

    private String bizNo;

    private String userId;

    private Integer amount;

    private BigDecimal balance;

    private BigDecimal frozenBalance;

    private BillingOperationStatus status;
}
