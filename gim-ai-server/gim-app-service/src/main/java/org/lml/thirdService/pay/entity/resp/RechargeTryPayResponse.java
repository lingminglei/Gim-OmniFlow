package org.lml.thirdService.pay.entity.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.order.constant.TradeOrderState;
import org.lml.thirdService.pay.constant.PayOrderState;

@Getter
@Setter
@AllArgsConstructor
/**
 * try 阶段返回给前端的充值支付信息。
 */
public class RechargeTryPayResponse {

    private String payOrderId;

    private String payUrl;

    public RechargeTryPayResponse(){

    }

}
