package org.lml.billing.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * TCC 确认扣减请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BillingConfirmDebitReq extends BillingBaseChangeReq {

    private static final long serialVersionUID = 1L;

    /**
     * TCC 事务号。
     */
    @NotBlank(message = "tccTransactionNo 不能为空")
    private String tccTransactionNo;
}
