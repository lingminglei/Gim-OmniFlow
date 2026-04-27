package org.lml.billing.resp;

import lombok.Data;
import org.lml.billing.enums.BillingOperationStatus;
import org.lml.billing.enums.BillingTccStatus;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * TCC 扣减统一响应。
 */
@Data
public class BillingTccResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestNo;

    private String tccTransactionNo;

    private String bizNo;

    private String userId;

    private Integer amount;

    private BillingTccStatus tccStatus;

    private BillingOperationStatus status;

    private BigDecimal balance;

    private BigDecimal frozenBalance;
}
