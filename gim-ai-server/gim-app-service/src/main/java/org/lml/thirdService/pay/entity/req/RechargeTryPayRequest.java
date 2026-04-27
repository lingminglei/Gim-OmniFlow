package org.lml.thirdService.pay.entity.req;

import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.pay.constant.PayChannel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
/**
 * 充值支付尝试请求。
 */
public class RechargeTryPayRequest {

    /**
     * 充值套餐编码，对应RechargePackage中的code。
     */
    @NotBlank(message = "packageCode is blank")
    private String packageCode;

    /**
     * 业务幂等号，用于避免重复创建充值单。
     */
    @NotBlank(message = "identifier is blank")
    private String identifier;

    /**
     * 支付渠道，当前支持MOCK，后续可扩展微信、支付宝。
     */
    @NotNull(message = "payChannel is null")
    private PayChannel payChannel;

    /**
     * 支付备注，透传到支付单记录中。
     */
    private String memo;
}
