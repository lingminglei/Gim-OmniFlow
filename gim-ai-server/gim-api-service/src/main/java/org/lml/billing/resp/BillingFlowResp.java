package org.lml.billing.resp;

import lombok.Data;
import org.lml.billing.enums.BillingChangeType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分流水明细响应。
 */
@Data
public class BillingFlowResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String userId;

    private String requestNo;

    private String bizNo;

    private String sourceSystem;

    private String tccTransactionNo;

    private BillingChangeType changeType;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private BigDecimal frozenBefore;

    private BigDecimal frozenAfter;

    private String remark;

    private Date createTime;
}
