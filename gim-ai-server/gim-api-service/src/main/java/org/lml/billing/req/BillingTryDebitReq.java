package org.lml.billing.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * TCC 预扣减请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BillingTryDebitReq extends BillingBaseChangeReq {

    private static final long serialVersionUID = 1L;

    /**
     * TCC 事务号。
     */
    @NotBlank(message = "tccTransactionNo 不能为空")
    private String tccTransactionNo;

    /**
     * 事务过期秒数。
     */
    @NotNull(message = "expireSeconds 不能为空")
    @Min(value = 1, message = "expireSeconds 必须大于 0")
    private Integer expireSeconds;
}
