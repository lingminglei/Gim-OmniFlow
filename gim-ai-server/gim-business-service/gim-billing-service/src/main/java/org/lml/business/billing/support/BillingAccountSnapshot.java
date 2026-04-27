package org.lml.business.billing.support;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户变更前后快照。
 */
@Data
@Builder
public class BillingAccountSnapshot {

    private String userId;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private BigDecimal frozenBefore;

    private BigDecimal frozenAfter;
}
