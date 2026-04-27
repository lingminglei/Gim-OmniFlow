package org.lml.thirdService.pay.entity.req;

import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.pay.constant.PayChannel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
/**
 * 充值支付确认请求。
 * 当前阶段只负责基于已创建订单发起支付，不处理最终支付成功回调。
 */
public class RechargeConfirmRequest {

    /**
     * 系统内部支付单号。
     */
    @NotBlank(message = "payOrderId is blank")
    private String payOrderId;

    /**
     * 本次发起支付选择的支付渠道。
     */
    @NotNull(message = "payChannel is null")
    private PayChannel payChannel;

    private String remark;
}
