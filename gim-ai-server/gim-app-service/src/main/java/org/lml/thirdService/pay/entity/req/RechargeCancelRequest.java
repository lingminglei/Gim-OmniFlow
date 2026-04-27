package org.lml.thirdService.pay.entity.req;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
/**
 * 充值支付取消请求。
 */
public class RechargeCancelRequest {
    /**
     * TCC事务号，用于标识本次需要回滚的支付事务。
     */
    @NotBlank(message = "transactionId is blank")
    private String transactionId;

    /**
     * 待取消的支付单号。
     */
    @NotBlank(message = "payOrderId is blank")
    private String payOrderId;

    /**
     * 取消原因，方便异步回滚链路记录和排查。
     */
    private String reason;
}
